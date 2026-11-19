package com.pagoflex.soporte.features.reporte;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.exceptions.CobroNoReportable;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import com.pagoflex.soporte.support.RelojFijo;
import com.pagoflex.soporte.support.TokensPrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Flujo del reporte: el usuario reporta y el agente lo toma y resuelve.
/// cobranza-service queda simulado para probar soporte solo
@SpringBootTest
@AutoConfigureMockMvc
@Import(RelojFijo.class)
@Transactional
class SoporteFlujoTest {
    private static final String FOLIO = "CP-2026-0001";
    private static final String CORREO_CAMILA = "crojas@correo.ejemplo";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReporteRepository repository;

    @MockitoBean
    private CobranzaServiceClient cobranza;

    private final UUID camila = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID jorge = UUID.randomUUID();
    private String tokenCamila;
    private String tokenAna;
    private String tokenJorge;

    @BeforeEach
    void prepararCobranza() {
        // 1. camila es usuaria final, ana y jorge son agentes de soporte
        tokenCamila = TokensPrueba.bearer(camila, CORREO_CAMILA, "USUARIO_FINAL");
        tokenAna = TokensPrueba.bearer(ana, "aperez@pagoflex.ejemplo", "AGENTE");
        tokenJorge = TokensPrueba.bearer(jorge, "jsoto@pagoflex.ejemplo", "AGENTE");

        // 2. el cobro de camila en cobranza
        when(cobranza.obtener(FOLIO)).thenReturn(cobro("VENCIDO"));
        when(cobranza.marcarEnRevision(FOLIO)).thenReturn(cobro("EN_REVISION"));
        when(cobranza.salirDeRevision(FOLIO)).thenReturn(cobro("VENCIDO"));
    }

    // ---------- seguridad ----------
    @Test
    void eachRoleOnlySeesItsOwnRoutes() throws Exception {
        mvc.perform(get("/api/v1/soporte/reportes/mios"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/soporte/bandeja").header("Authorization", tokenCamila))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/soporte/reportes/mios").header("Authorization", tokenAna))
                .andExpect(status().isForbidden());
    }

    // ---------- reporte del usuario RN-09 ----------
    @Test
    void userReportsHisChargeAndItGoesToReview() throws Exception {
        reportar(tokenCamila, "YA_PAGUE", "Pagué por transferencia el 30-09")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("REP-0001"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.atrasado").value(false))
                .andExpect(jsonPath("$.cobro.empresa").value("Inmobiliaria Altos del Maipo"))
                .andExpect(jsonPath("$.cobro.total").value(108500))
                .andExpect(jsonPath("$.cobro.numeroCuota").value(1))
                .andExpect(jsonPath("$.cobro.estado").value("EN_REVISION"))
                .andExpect(jsonPath("$.usuario.correo").value(CORREO_CAMILA));

        verify(cobranza).marcarEnRevision(FOLIO);

        mvc.perform(get("/api/v1/soporte/reportes/mios").header("Authorization", tokenCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].folio").value(FOLIO));
    }

    @Test
    void otherReasonNeedsAComment() throws Exception {
        reportar(tokenCamila, "OTRO", "  ")
                .andExpect(status().isBadRequest());
        verify(cobranza, never()).marcarEnRevision(anyString());
    }

    @Test
    void cannotReportSomeoneElsesCharge() throws Exception {
        String otro = TokensPrueba.bearer(UUID.randomUUID(), "otra@correo.ejemplo", "USUARIO_FINAL");
        reportar(otro, "NO_RECONOZCO", null)
                .andExpect(status().isNotFound());
        verify(cobranza, never()).marcarEnRevision(anyString());
    }

    @Test
    void cannotReportTheSameChargeTwice() throws Exception {
        reportar(tokenCamila, "YA_PAGUE", null).andExpect(status().isCreated());
        reportar(tokenCamila, "MONTO_INCORRECTO", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Ya tienes un reporte abierto de este cobro: REP-0001"));
    }

    @Test
    void cobranzaErrorsAreReturnedToTheUser() throws Exception {
        when(cobranza.obtener("CP-2026-0404")).thenThrow(new CompromisoNoEncontrado("CP-2026-0404"));
        mvc.perform(post("/api/v1/soporte/reportes").header("Authorization", tokenCamila)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"CP-2026-0404\",\"motivo\":\"YA_PAGUE\"}"))
                .andExpect(status().isNotFound());

        // un cobro pagado o anulado no se puede reportar
        when(cobranza.marcarEnRevision(FOLIO)).thenThrow(new CobroNoReportable("El cobro ya está pagado"));
        reportar(tokenCamila, "YA_PAGUE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("El cobro ya está pagado"));
    }

    // ---------- bandeja del agente RN-23 ----------
    @Test
    void lateReportsGoFirstInTheInbox() throws Exception {
        // 1. un reporte del viernes (2 días hábiles) y otro del jueves (3, atrasado)
        guardar("REP-0001", LocalDateTime.of(2026, 10, 2, 9, 0));
        guardar("REP-0002", LocalDateTime.of(2026, 10, 1, 18, 0));

        mvc.perform(get("/api/v1/soporte/bandeja").header("Authorization", tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].numero").value("REP-0002"))
                .andExpect(jsonPath("$[0].atrasado").value(true))
                .andExpect(jsonPath("$[1].atrasado").value(false));

        mvc.perform(get("/api/v1/soporte/bandeja").param("estado", "RESUELTO").header("Authorization", tokenAna))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void agentTakesAndResolvesTheReport() throws Exception {
        reportar(tokenCamila, "YA_PAGUE", null).andExpect(status().isCreated());

        // 1. no se puede resolver sin tomarlo
        resolver(tokenAna, "Revisado").andExpect(status().isConflict());

        // 2. ana lo toma y jorge ya no puede
        mvc.perform(post("/api/v1/soporte/bandeja/REP-0001/tomar").header("Authorization", tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_GESTION"))
                .andExpect(jsonPath("$.agente").value("aperez@pagoflex.ejemplo"));
        mvc.perform(post("/api/v1/soporte/bandeja/REP-0001/tomar").header("Authorization", tokenJorge))
                .andExpect(status().isConflict());

        // 3. solo la responsable resuelve y la respuesta es obligatoria
        resolver(tokenJorge, "Revisado").andExpect(status().isForbidden());
        resolver(tokenAna, " ").andExpect(status().isBadRequest());
        verify(cobranza, never()).salirDeRevision(anyString());

        resolver(tokenAna, "Confirmamos el pago con la empresa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.cobro.estado").value("VENCIDO"))
                .andExpect(jsonPath("$.respuesta").value("Confirmamos el pago con la empresa"));
        verify(cobranza).salirDeRevision(FOLIO);

        // 4. ya resuelto se puede volver a reportar
        reportar(tokenCamila, "MONTO_INCORRECTO", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("REP-0002"));
    }

    @Test
    void unknownReportIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/soporte/bandeja/REP-9999").header("Authorization", tokenAna))
                .andExpect(status().isNotFound());
    }

    private ResultActions reportar(String token, String motivo, String comentario) throws Exception {
        String cuerpo = comentario == null
                ? "{\"folio\":\"%s\",\"motivo\":\"%s\"}".formatted(FOLIO, motivo)
                : "{\"folio\":\"%s\",\"motivo\":\"%s\",\"comentario\":\"%s\"}".formatted(FOLIO, motivo, comentario);
        return mvc.perform(post("/api/v1/soporte/reportes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions resolver(String token, String respuesta) throws Exception {
        return mvc.perform(post("/api/v1/soporte/bandeja/REP-0001/resolver").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuesta\":\"%s\"}".formatted(respuesta)));
    }

    private void guardar(String numero, LocalDateTime creacion) {
        repository.save(Reporte.builder()
                .numero(numero)
                .folio("PR-" + numero)
                .motivo(MotivoReporte.NO_RECONOZCO)
                .estado(EstadoReporte.ABIERTO)
                .fechaCreacion(creacion)
                .empresa("Inmobiliaria Altos del Maipo")
                .concepto("Gasto común")
                .total(100000)
                .fechaVencimiento(LocalDate.of(2026, 9, 30))
                .nombreUsuario("Camila Rojas")
                .correoUsuario(CORREO_CAMILA)
                .build());
    }

    private static CompromisoInterno cobro(String estado) {
        return new CompromisoInterno(FOLIO, "Inmobiliaria Altos del Maipo", "Gasto común octubre", 1, 1,
                108500, LocalDate.of(2026, 10, 1), estado, "Camila Rojas", CORREO_CAMILA, "+56 9 1234 5678");
    }
}
