package com.pagoflex.soporte.features.reporte;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.exceptions.CobranzaNoDisponible;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.core.exceptions.EjecutivoSinEmpresa;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import com.pagoflex.soporte.support.RedisPrueba;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Flujo del reporte: el usuario reporta, el agente lo toma, lo atiende y lo resuelve.
/// cobranza-service queda simulado para probar soporte solo
@SpringBootTest
@AutoConfigureMockMvc
@Import({RelojFijo.class, RedisPrueba.class})
@Transactional
class SoporteFlujoTest {
    private static final String FOLIO = "CP-2026-0001";
    private static final String CORREO_CAMILA = "crojas@correo.ejemplo";
    private static final UUID EMPRESA_MAIPO = UUID.randomUUID();
    private static final UUID EMPRESA_OTRA = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReporteRepository repository;

    @MockitoBean
    private CobranzaServiceClient cobranza;

    private final UUID camila = UUID.randomUUID();
    private final UUID ana = UUID.randomUUID();
    private final UUID jorge = UUID.randomUUID();
    private final UUID paula = UUID.randomUUID();
    private String tokenCamila;
    private String tokenAna;
    private String tokenJorge;
    private String tokenPaula;

    @BeforeEach
    void prepararCobranza() {
        // 1. camila es usuaria final, ana y jorge agentes y paula ejecutiva de altos del maipo
        tokenCamila = TokensPrueba.bearer(camila, CORREO_CAMILA, "USUARIO_FINAL");
        tokenAna = TokensPrueba.bearer(ana, "aperez@pagoflex.ejemplo", "AGENTE");
        tokenJorge = TokensPrueba.bearer(jorge, "jsoto@pagoflex.ejemplo", "AGENTE");
        tokenPaula = TokensPrueba.bearer(paula, "psaavedra@altosmaipo.ejemplo", "EJECUTIVO");

        // 2. el cobro de camila en cobranza; para cualquier otro usuario es ajeno
        when(cobranza.reportabilidad(eq(FOLIO), any())).thenReturn(cobro("VENCIDO", true, false));
        when(cobranza.reportabilidad(FOLIO, camila)).thenReturn(cobro("VENCIDO", true, true));
        when(cobranza.marcarEnRevision(FOLIO)).thenReturn(cobro("EN_REVISION", false, null));
        when(cobranza.finRevision(FOLIO)).thenReturn(cobro("VENCIDO", true, null));
        when(cobranza.empresaDeEjecutivo(paula)).thenReturn(new EmpresaInterna(EMPRESA_MAIPO, "EC-01", "Altos del Maipo"));
    }

    // ---------- seguridad ----------
    @Test
    void aTokenWithoutALiveRedisSessionIsRejected() throws Exception {
        mvc.perform(get("/api/v1/reportes/me").header("Authorization", tokenCamila)).andExpect(status().isOk());

        // al cerrar sesión usuarios borra las claves de redis y el access token deja de valer
        TokensPrueba.SESIONES.clear();

        mvc.perform(get("/api/v1/reportes/me").header("Authorization", tokenCamila)).andExpect(status().isUnauthorized());
    }

    @Test
    void eachRoleOnlySeesItsOwnRoutes() throws Exception {
        mvc.perform(get("/api/v1/reportes/me"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/reportes").header("Authorization", tokenCamila))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes/me").header("Authorization", tokenAna))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/reportes").header("Authorization", tokenAna)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"%s\",\"motivo\":\"YA_PAGUE\"}".formatted(FOLIO)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/empresas/me/reportes").header("Authorization", tokenAna))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/reportes").header("Authorization", tokenPaula))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalRouteNeedsTheKey() throws Exception {
        guardar("REP-0001", LocalDateTime.of(2026, 10, 5, 9, 0), EMPRESA_MAIPO);

        mvc.perform(get("/api/v1/internal/reportes/REP-0001/participantes"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internal/reportes/REP-0001/participantes").header("X-Internal-Api-Key", "otra"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/internal/reportes/REP-0001/participantes")
                        .header("X-Internal-Api-Key", TokensPrueba.CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.empresaId").value(EMPRESA_MAIPO.toString()));
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
                .andExpect(jsonPath("$.usuario.correo").value(CORREO_CAMILA))
                .andExpect(jsonPath("$.historial", hasSize(1)))
                .andExpect(jsonPath("$.historial[0].estadoNuevo").value("ABIERTO"));

        verify(cobranza).marcarEnRevision(FOLIO);

        mvc.perform(get("/api/v1/reportes/me").header("Authorization", tokenCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].folio").value(FOLIO))
                .andExpect(jsonPath("$.totalElementos").value(1));
        mvc.perform(get("/api/v1/reportes/me/REP-0001").header("Authorization", tokenCamila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("REP-0001"));

        // otro usuario no ve el reporte de camila
        String otro = TokensPrueba.bearer(UUID.randomUUID(), "otra@correo.ejemplo", "USUARIO_FINAL");
        mvc.perform(get("/api/v1/reportes/me/REP-0001").header("Authorization", otro))
                .andExpect(status().isNotFound());
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
    void paidOrCancelledChargeCannotBeReported() throws Exception {
        when(cobranza.reportabilidad(FOLIO, camila)).thenReturn(cobro("PAGADO", false, true));
        reportar(tokenCamila, "YA_PAGUE", null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Este cobro está PAGADO y no se puede reportar"));
        verify(cobranza, never()).marcarEnRevision(anyString());

        when(cobranza.reportabilidad("CP-2026-0404", camila)).thenThrow(new CompromisoNoEncontrado("CP-2026-0404"));
        mvc.perform(post("/api/v1/reportes").header("Authorization", tokenCamila)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"folio\":\"CP-2026-0404\",\"motivo\":\"YA_PAGUE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void myReportsArePagedWithAMaxSize() throws Exception {
        for (int i = 1; i <= 3; i++) {
            Reporte r = guardar("REP-000" + i, LocalDateTime.of(2026, 10, i, 9, 0), EMPRESA_MAIPO);
            r.setUsuarioId(camila);
            repository.save(r);
        }

        mvc.perform(get("/api/v1/reportes/me").param("page", "0").param("size", "2")
                        .header("Authorization", tokenCamila))
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0003"))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/v1/reportes/me").param("page", "1").param("size", "2")
                        .header("Authorization", tokenCamila))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.pagina").value(1));
        // un tamaño enorme se recorta a 50
        mvc.perform(get("/api/v1/reportes/me").param("size", "500").header("Authorization", tokenCamila))
                .andExpect(jsonPath("$.tamano").value(50));
    }

    // ---------- bandeja del agente RN-23 ----------
    @Test
    void lateReportsGoFirstAndOtherAgentsReportsAreHidden() throws Exception {
        // 1. uno del viernes (2 días hábiles), uno del jueves (atrasado) y uno tomado por jorge
        guardar("REP-0001", LocalDateTime.of(2026, 10, 2, 9, 0), EMPRESA_MAIPO);
        guardar("REP-0002", LocalDateTime.of(2026, 10, 1, 18, 0), EMPRESA_MAIPO);
        Reporte deJorge = guardar("REP-0003", LocalDateTime.of(2026, 9, 30, 9, 0), EMPRESA_MAIPO);
        deJorge.setAgenteId(jorge);
        deJorge.registrarCambio(EstadoReporte.ASIGNADO, jorge, "jsoto@pagoflex.ejemplo", LocalDateTime.of(2026, 10, 1, 9, 0));
        repository.save(deJorge);

        mvc.perform(get("/api/v1/reportes").header("Authorization", tokenAna))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0002"))
                .andExpect(jsonPath("$.contenido[0].atrasado").value(true))
                .andExpect(jsonPath("$.contenido[1].atrasado").value(false));
        mvc.perform(get("/api/v1/reportes").param("atrasado", "true").header("Authorization", tokenAna))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0002"));
        mvc.perform(get("/api/v1/reportes").param("estado", "RESUELTO").header("Authorization", tokenAna))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        // 2. jorge ve también el suyo; ana no puede abrir el de jorge
        mvc.perform(get("/api/v1/reportes").header("Authorization", tokenJorge))
                .andExpect(jsonPath("$.totalElementos").value(3));
        mvc.perform(get("/api/v1/reportes/REP-0003").header("Authorization", tokenAna))
                .andExpect(status().isNotFound());
    }

    @Test
    void agentAssignsAttendsAndResolvesTheReport() throws Exception {
        reportar(tokenCamila, "YA_PAGUE", null).andExpect(status().isCreated());

        // 1. sin tomarlo no se atiende ni se resuelve
        accion(tokenAna, "atenciones").andExpect(status().isConflict());
        resolver(tokenAna, "Revisado").andExpect(status().isConflict());

        // 2. ana lo toma y jorge ya no puede
        accion(tokenAna, "asignaciones")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ASIGNADO"))
                .andExpect(jsonPath("$.agente").value("aperez@pagoflex.ejemplo"));
        accion(tokenJorge, "asignaciones").andExpect(status().isConflict());

        // 3. no se puede saltar a resuelto sin iniciar la atención
        resolver(tokenAna, "Revisado").andExpect(status().isConflict());
        accion(tokenJorge, "atenciones").andExpect(status().isForbidden());
        accion(tokenAna, "atenciones")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("EN_ATENCION"));
        accion(tokenAna, "atenciones").andExpect(status().isConflict());

        // 4. solo la responsable resuelve y la respuesta es obligatoria
        resolver(tokenJorge, "Revisado").andExpect(status().isForbidden());
        resolver(tokenAna, " ").andExpect(status().isBadRequest());
        verify(cobranza, never()).finRevision(anyString());

        resolver(tokenAna, "Confirmamos el pago con la empresa")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.cobro.estado").value("VENCIDO"))
                .andExpect(jsonPath("$.respuesta").value("Confirmamos el pago con la empresa"))
                .andExpect(jsonPath("$.historial", hasSize(4)))
                .andExpect(jsonPath("$.historial[1].estadoAnterior").value("ABIERTO"))
                .andExpect(jsonPath("$.historial[1].estadoNuevo").value("ASIGNADO"))
                .andExpect(jsonPath("$.historial[2].estadoNuevo").value("EN_ATENCION"))
                .andExpect(jsonPath("$.historial[3].estadoNuevo").value("RESUELTO"))
                .andExpect(jsonPath("$.historial[3].actor").value("aperez@pagoflex.ejemplo"));
        verify(cobranza).finRevision(FOLIO);
        resolver(tokenAna, "Otra vez").andExpect(status().isConflict());

        // 5. ya resuelto se puede volver a reportar
        reportar(tokenCamila, "MONTO_INCORRECTO", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numero").value("REP-0002"));
    }

    @Test
    void reportIsNotResolvedIfCobranzaFails() throws Exception {
        reportar(tokenCamila, "YA_PAGUE", null).andExpect(status().isCreated());
        accion(tokenAna, "asignaciones").andExpect(status().isOk());
        accion(tokenAna, "atenciones").andExpect(status().isOk());

        when(cobranza.finRevision(FOLIO)).thenThrow(new CobranzaNoDisponible());
        resolver(tokenAna, "Listo").andExpect(status().isServiceUnavailable());

        mvc.perform(get("/api/v1/reportes/REP-0001").header("Authorization", tokenAna))
                .andExpect(jsonPath("$.estado").value("EN_ATENCION"))
                .andExpect(jsonPath("$.respuesta").doesNotExist());
    }

    @Test
    void unknownReportIsNotFound() throws Exception {
        mvc.perform(get("/api/v1/reportes/REP-9999").header("Authorization", tokenAna))
                .andExpect(status().isNotFound());
    }

    // ---------- ejecutivo de la empresa ----------
    @Test
    void executiveOnlySeesReportsOfHisCompany() throws Exception {
        guardar("REP-0001", LocalDateTime.of(2026, 10, 2, 9, 0), EMPRESA_MAIPO);
        guardar("REP-0002", LocalDateTime.of(2026, 10, 3, 9, 0), EMPRESA_OTRA);

        mvc.perform(get("/api/v1/empresas/me/reportes").header("Authorization", tokenPaula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0001"));
        mvc.perform(get("/api/v1/empresas/me/reportes").param("estado", "RESUELTO").header("Authorization", tokenPaula))
                .andExpect(jsonPath("$.contenido", hasSize(0)));

        // reporte activo del cobro: el de su empresa sí, el de otra no
        mvc.perform(get("/api/v1/empresas/me/compromisos/PR-REP-0001/reporte-activo").header("Authorization", tokenPaula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("REP-0001"));
        mvc.perform(get("/api/v1/empresas/me/compromisos/PR-REP-0002/reporte-activo").header("Authorization", tokenPaula))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/empresas/me/compromisos/CP-2026-0999/reporte-activo").header("Authorization", tokenPaula))
                .andExpect(status().isNotFound());

        // un ejecutivo sin empresa no ve nada
        UUID sinEmpresa = UUID.randomUUID();
        when(cobranza.empresaDeEjecutivo(sinEmpresa)).thenThrow(new EjecutivoSinEmpresa());
        mvc.perform(get("/api/v1/empresas/me/reportes")
                        .header("Authorization", TokensPrueba.bearer(sinEmpresa, "x@empresa.ejemplo", "EJECUTIVO")))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---
    private ResultActions reportar(String token, String motivo, String comentario) throws Exception {
        String cuerpo = comentario == null
                ? "{\"folio\":\"%s\",\"motivo\":\"%s\"}".formatted(FOLIO, motivo)
                : "{\"folio\":\"%s\",\"motivo\":\"%s\",\"comentario\":\"%s\"}".formatted(FOLIO, motivo, comentario);
        return mvc.perform(post("/api/v1/reportes").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(cuerpo));
    }

    private ResultActions accion(String token, String accion) throws Exception {
        return mvc.perform(post("/api/v1/reportes/REP-0001/" + accion).header("Authorization", token));
    }

    private ResultActions resolver(String token, String respuesta) throws Exception {
        return mvc.perform(post("/api/v1/reportes/REP-0001/resoluciones").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"respuesta\":\"%s\"}".formatted(respuesta)));
    }

    private Reporte guardar(String numero, LocalDateTime creacion, UUID empresaId) {
        Reporte r = Reporte.builder()
                .numero(numero)
                .folio("PR-" + numero)
                .motivo(MotivoReporte.NO_RECONOZCO)
                .fechaCreacion(creacion)
                .usuarioId(UUID.randomUUID())
                .empresaId(empresaId)
                .empresa("Inmobiliaria Altos del Maipo")
                .concepto("Gasto común")
                .total(100000)
                .fechaVencimiento(LocalDate.of(2026, 9, 30))
                .nombreUsuario("Camila Rojas")
                .usuarioCorreo(CORREO_CAMILA)
                .build();
        r.registrarCambio(EstadoReporte.ABIERTO, r.getUsuarioId(), CORREO_CAMILA, creacion);
        return repository.save(r);
    }

    private static CompromisoInterno cobro(String estado, boolean reportable, Boolean delUsuario) {
        return new CompromisoInterno(FOLIO, UUID.randomUUID(), EMPRESA_MAIPO, "Inmobiliaria Altos del Maipo",
                "Gasto común octubre", 1, 1, 108500, LocalDate.of(2026, 10, 1), estado, "15.234.876-1",
                "Camila Rojas", CORREO_CAMILA, "+56 9 1234 5678", reportable, delUsuario);
    }
}
