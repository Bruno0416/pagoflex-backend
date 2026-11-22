package com.pagoflex.cobranza.features.compromiso;

import com.pagoflex.cobranza.core.client.ReconocimientosClient;
import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.ComprobanteRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PagoRepository;
import com.pagoflex.cobranza.features.empresa.data.CargaEmpresas;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.cobranza.support.RedisPrueba;
import com.pagoflex.cobranza.support.RelojFijo;
import com.pagoflex.contracts.organizacion.IdentificadoresEmpresa;
import com.pagoflex.contracts.usuarios.SolicitudCrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.pagoflex.cobranza.support.TokensPrueba.CLAVE_INTERNA;
import static com.pagoflex.cobranza.support.TokensPrueba.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({RelojFijo.class, RedisPrueba.class})
class CobranzaFlujoTest {
    private static final UUID EJ_ALTOS = UUID.randomUUID();
    private static final UUID EJ_CREDITO = UUID.randomUUID();
    private static final UUID EJ_SIN_EMPRESA = UUID.randomUUID();
    private static final UUID U_CAMILA = UUID.randomUUID();
    private static final UUID U_LUIS = UUID.randomUUID();
    private static final PersonaInterna CAMILA = new PersonaInterna(UUID.randomUUID(), "15834207-3",
            "Camila Andrea", "Rojas Peña", "crojas@correo.ejemplo", "+56 9 0000 0101", U_CAMILA);
    private static final PersonaInterna LUIS = new PersonaInterna(UUID.randomUUID(), "12476318-5",
            "Luis Alberto", "Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102", U_LUIS);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CompromisoRepository compromisoRepo;
    @Autowired
    private PagoRepository pagoRepo;
    @Autowired
    private ComprobanteRepository comprobanteRepo;
    @Autowired
    private AnulacionRepository anulacionRepo;
    @Autowired
    private EmpresaClienteRepository empresaRepo;

    @MockitoBean
    private UsuariosServiceClient usuarios;
    @MockitoBean
    private ReconocimientosClient reconocimientos;

    @BeforeEach
    void preparar() {
        comprobanteRepo.deleteAll();
        pagoRepo.deleteAll();
        anulacionRepo.deleteAll();
        compromisoRepo.deleteAll();
        new CargaEmpresas(empresaRepo).run(null);

        // la empresa de cada ejecutivo es la de su persona en usuarios
        ejecutivo(EJ_ALTOS, "psaavedra@altosmaipo.ejemplo", "EC-01");
        ejecutivo(EJ_CREDITO, "chenriquez@creditoandino.ejemplo", "EC-03");
        when(usuarios.porUsuario(EJ_SIN_EMPRESA)).thenReturn(Optional.of(new PersonaInterna(UUID.randomUUID(),
                "9999999-9", "Sin", "Empresa", "x@y.cl", null, EJ_SIN_EMPRESA, null)));

        for (PersonaInterna p : new PersonaInterna[]{CAMILA, LUIS}) {
            when(usuarios.porUsuario(p.usuarioId())).thenReturn(Optional.of(p));
            when(usuarios.porId(p.id())).thenReturn(Optional.of(p));
            when(usuarios.porRut(p.rut())).thenReturn(Optional.of(p));
        }
    }

    private void ejecutivo(UUID usuarioId, String correo, String codigoEmpresa) {
        UUID empresaId = empresaRepo.findByCodigo(codigoEmpresa).orElseThrow().getId();
        when(usuarios.porUsuario(usuarioId)).thenReturn(Optional.of(new PersonaInterna(UUID.randomUUID(),
                "1111111-1", "Ejecutivo", codigoEmpresa, correo, null, usuarioId, empresaId)));
    }

    // ---------- seguridad ----------
    @Test
    void cadaPerfilSoloEntraASusRutas() throws Exception {
        mvc.perform(get("/api/v1/empresas/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", camila())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/compromisos/me").header("Authorization", altos())).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", bearer(EJ_SIN_EMPRESA, "x@y.cl", "EJECUTIVO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internal/compromisos/CP-2026-0001/reportabilidad")).andExpect(status().isUnauthorized());
    }

    // ---------- ejecutivo ----------
    @Test
    void ejecutivoRegistraConPersonaExistenteYNoDuplica() throws Exception {
        String cuerpo = registro("15.834.207-3", "Cuota pie departamento", 16, 36, "2026-10-30");
        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folio").value("CP-2026-0001"))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.persona.nombre").value("Camila Andrea Rojas Peña"))
                .andExpect(jsonPath("$.registradoPor").value("psaavedra@altosmaipo.ejemplo"));

        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(cuerpo.replace("Cuota pie departamento", "CUOTA PIE departamento")))
                .andExpect(status().isConflict());

        // otra empresa no lo ve RN-22
        mvc.perform(get("/api/v1/empresas/me/compromisos/CP-2026-0001").header("Authorization", credito()))
                .andExpect(status().isNotFound());
        verify(usuarios, never()).crear(any());
    }

    @Test
    void personaNuevaPideSusDatosYSeCreaEnUsuarios() throws Exception {
        when(usuarios.porRut("20111222-2")).thenReturn(Optional.empty());
        when(usuarios.crear(any(SolicitudCrearPersonaInterna.class))).thenAnswer(i -> {
            SolicitudCrearPersonaInterna r = i.getArgument(0);
            return new PersonaInterna(UUID.randomUUID(), r.rut(), r.nombres(), r.apellidos(), r.correoContacto(), r.telefono(), null);
        });

        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(registro("20.111.222-2", "Arriendo", null, null, "2026-10-30")))
                .andExpect(status().isBadRequest());

        String conDatos = """
                {"rut":"20.111.222-2","nombres":"Ana","apellidos":"Pérez","correo":"ana@correo.ejemplo",
                 "telefono":"+56 9 1111 2222","concepto":"Arriendo","monto":250000,"fechaVencimiento":"2026-10-30"}
                """;
        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(conDatos))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.persona.rut").value("20111222-2"));
        verify(usuarios).crear(any(SolicitudCrearPersonaInterna.class));
    }

    @Test
    void validaRutFechaYCuotas() throws Exception {
        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(registro("12345678-9", "x", null, null, "2026-10-30")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(registro("15834207-3", "x", null, null, "2026-10-01")))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/empresas/me/compromisos").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content(registro("15834207-3", "x", 5, 3, "2026-10-30")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/empresas/me/personas/15.834.207-3").header("Authorization", altos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("crojas@correo.ejemplo"));
    }

    @Test
    void resumenYAnulacion() throws Exception {
        compromiso("CP-2026-0101", CAMILA, "EC-01", "Cuota", 1, 3, 100_000, "2026-09-10", EstadoCompromiso.PAGADO);
        compromiso("CP-2026-0102", LUIS, "EC-01", "Cuota", 1, 3, 100_000, "2026-09-20", EstadoCompromiso.PENDIENTE);
        compromiso("CP-2026-0103", LUIS, "EC-01", "Otro", null, null, 50_000, "2026-09-25", EstadoCompromiso.PENDIENTE);

        mvc.perform(post("/api/v1/empresas/me/compromisos/CP-2026-0101/anulaciones").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"ERROR_REGISTRO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/empresas/me/compromisos/CP-2026-0103/anulaciones").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"OTRO\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/v1/empresas/me/compromisos/CP-2026-0103/anulaciones").header("Authorization", altos())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"OTRO\",\"comentario\":\"duplicado\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ANULADO"))
                .andExpect(jsonPath("$.anulacion.ejecutivo").value("psaavedra@altosmaipo.ejemplo"));

        // 1 pagado de 3, menos 1 anulado: 50%; el pendiente de septiembre ya está vencido
        mvc.perform(get("/api/v1/empresas/me/cobranza?mes=2026-09").header("Authorization", altos()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCompromisos").value(3))
                .andExpect(jsonPath("$.vencidos").value(1))
                .andExpect(jsonPath("$.tasaCobranza").value(50.0));
        mvc.perform(get("/api/v1/empresas/me/compromisos?mes=2026-09&estado=VENCIDO").header("Authorization", altos()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].recargo").value(8_500));
    }

    // ---------- usuario final ----------
    @Test
    void usuarioSoloVeLoSuyo() throws Exception {
        compromiso("CP-2026-0201", CAMILA, "EC-03", "Crédito", 1, 2, 80_000, "2026-10-25", EstadoCompromiso.PENDIENTE);
        compromiso("CP-2026-0202", LUIS, "EC-01", "Cuota pie", 1, 36, 310_000, "2026-10-30", EstadoCompromiso.PENDIENTE);

        mvc.perform(get("/api/v1/compromisos/me").header("Authorization", camila()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].folio").value("CP-2026-0201"))
                .andExpect(jsonPath("$[0].pagable").value(true));
        mvc.perform(get("/api/v1/compromisos/me/CP-2026-0202").header("Authorization", camila()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/compromisos/me/resumen?mes=2026-10").header("Authorization", camila()))
                .andExpect(jsonPath("$.porPagarMes").value(80_000))
                .andExpect(jsonPath("$.proximos", hasSize(1)));
    }

    @Test
    void pagoEnOrdenConComprobanteEIdempotencia() throws Exception {
        compromiso("CP-2026-0301", CAMILA, "EC-03", "Crédito de consumo", 7, 24, 86_400, "2026-10-05", EstadoCompromiso.PENDIENTE);
        compromiso("CP-2026-0302", CAMILA, "EC-03", "Crédito de consumo", 8, 24, 86_400, "2026-11-05", EstadoCompromiso.PENDIENTE);

        // 1. la cuota 8 espera a la 7 RN-04
        mvc.perform(get("/api/v1/compromisos/me/CP-2026-0302/cotizacion-pago").header("Authorization", camila()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PAYMENT_OUT_OF_ORDER"))
                .andExpect(jsonPath("$.details.folioAnterior").value("CP-2026-0301"));

        // 2. la 7 venció ayer: total con el recargo de crédito andino
        String cotizacion = campo(mvc.perform(get("/api/v1/compromisos/me/CP-2026-0301/cotizacion-pago").header("Authorization", camila()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recargo").value(12_000))
                .andExpect(jsonPath("$.total").value(98_400)), "cotizacionId");

        // 3. cotización vieja o sin llave se rechaza
        mvc.perform(pagar("CP-2026-0301", "llave-1", "otra")).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUOTE_CHANGED"));
        mvc.perform(post("/api/v1/compromisos/me/CP-2026-0301/pagos").header("Authorization", camila())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"medioPago\":\"DEBITO\",\"cotizacionId\":\"" + cotizacion + "\"}"))
                .andExpect(status().isBadRequest());

        // 4. pago con comprobante; repetir la llave devuelve lo mismo sin cobrar de nuevo
        mvc.perform(pagar("CP-2026-0301", "llave-1", cotizacion)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroComprobante").value("CPR-000001"))
                .andExpect(jsonPath("$.montoPagado").value(98_400))
                .andExpect(jsonPath("$.canal").value("APP"));
        mvc.perform(pagar("CP-2026-0301", "llave-1", cotizacion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroComprobante").value("CPR-000001"));
        mvc.perform(pagar("CP-2026-0301", "llave-2", cotizacion)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_PAID"));
        assertThat(pagoRepo.count()).isEqualTo(1);

        // 5. ahora la cuota 8 se puede pagar y el historial muestra el pago
        mvc.perform(get("/api/v1/compromisos/me/CP-2026-0302").header("Authorization", camila()))
                .andExpect(jsonPath("$.pagable").value(true));
        mvc.perform(get("/api/v1/pagos/me").header("Authorization", camila()))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].folio").value("CP-2026-0301"));
        mvc.perform(get("/api/v1/comprobantes/CPR-000001").header("Authorization", camila()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personaRut").value("15834207-3"))
                .andExpect(jsonPath("$.recargo").value(12_000));
        mvc.perform(get("/api/v1/comprobantes/CPR-000001").header("Authorization", luis()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/compromisos/me/CP-2026-0301").header("Authorization", camila()))
                .andExpect(jsonPath("$.estado").value("PAGADO"))
                .andExpect(jsonPath("$.numeroComprobante").value("CPR-000001"));
    }

    @Test
    void cumplimientoYAnulacionesDelUsuario() throws Exception {
        compromiso("CP-2026-0401", CAMILA, "EC-06", "Cuota social", null, null, 15_000, "2026-09-05", EstadoCompromiso.VENCIDO);
        mvc.perform(get("/api/v1/cumplimiento/me").header("Authorization", camila()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elegibles").value(1))
                .andExpect(jsonPath("$.porcentaje").doesNotExist());
        mvc.perform(get("/api/v1/anulaciones/me").header("Authorization", camila()))
                .andExpect(jsonPath("$", hasSize(0)));
        // cuenta sin persona en usuarios
        when(usuarios.porUsuario(U_LUIS)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/compromisos/me").header("Authorization", luis())).andExpect(status().isNotFound());
    }

    // ---------- rutas internas para soporte ----------
    @Test
    void revisionDesdeSoporteBloqueaElPagoYEsIdempotente() throws Exception {
        compromiso("CP-2026-0501", CAMILA, "EC-01", "Cuota pie", 1, 36, 310_000, "2026-10-30", EstadoCompromiso.PENDIENTE);
        compromiso("CP-2026-0502", CAMILA, "EC-01", "Cuota pie", 2, 36, 310_000, "2026-09-01", EstadoCompromiso.PAGADO);

        mvc.perform(get("/api/v1/internal/compromisos/CP-2026-0501/reportabilidad?usuarioId=" + U_CAMILA)
                        .header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delUsuario").value(true))
                .andExpect(jsonPath("$.reportable").value(true))
                .andExpect(jsonPath("$.correoPersona").value("crojas@correo.ejemplo"));
        mvc.perform(get("/api/v1/internal/compromisos/CP-2026-0501/reportabilidad?usuarioId=" + U_LUIS)
                        .header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(jsonPath("$.delUsuario").value(false));
        mvc.perform(get("/api/v1/internal/compromisos/CP-2026-9999/reportabilidad").header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isNotFound());

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/internal/compromisos/CP-2026-0501/revision").header("X-Internal-Api-Key", CLAVE_INTERNA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("EN_REVISION"));
        }
        mvc.perform(get("/api/v1/compromisos/me/CP-2026-0501/cotizacion-pago").header("Authorization", camila()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IN_REVIEW"));
        mvc.perform(put("/api/v1/internal/compromisos/CP-2026-0502/revision").header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isConflict());

        for (int i = 0; i < 2; i++) {
            mvc.perform(put("/api/v1/internal/compromisos/CP-2026-0501/fin-revision").header("X-Internal-Api-Key", CLAVE_INTERNA))
                    .andExpect(jsonPath("$.estado").value("PENDIENTE"));
        }
    }

    @Test
    void empresaDelEjecutivoParaOtrosServicios() throws Exception {
        mvc.perform(get("/api/v1/internal/empresas/ejecutivos/" + EJ_ALTOS).header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigo").value("EC-01"));
        mvc.perform(get("/api/v1/internal/empresas/ejecutivos/" + EJ_SIN_EMPRESA).header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isNotFound());
    }

    @Test
    void unTokenSinSesionVigenteEnRedisSeRechazaComoEnUsuarios() throws Exception {
        String token = camila();
        mvc.perform(get("/api/v1/compromisos/me").header("Authorization", token)).andExpect(status().isOk());

        // al cerrar la sesión usuarios borra las claves de redis y el access token deja de valer
        com.pagoflex.cobranza.support.TokensPrueba.SESIONES.clear();

        mvc.perform(get("/api/v1/compromisos/me").header("Authorization", token)).andExpect(status().isUnauthorized());
    }

    // ---------- integración con reconocimientos ----------
    @Test
    void hechosDePagoSoloConClaveInternaYConLosDatosRealesDelPago() throws Exception {
        compromiso("CP-2026-0401", CAMILA, "EC-03", "Crédito", 1, 2, 80_000, "2026-10-25", EstadoCompromiso.PENDIENTE);
        compromiso("CP-2026-0402", CAMILA, "EC-03", "Crédito", 2, 2, 80_000, "2026-11-25", EstadoCompromiso.PENDIENTE);
        String cotizacion = campo(mvc.perform(get("/api/v1/compromisos/me/CP-2026-0401/cotizacion-pago")
                .header("Authorization", camila())), "cotizacionId");
        mvc.perform(pagar("CP-2026-0401", "llave-hechos", cotizacion)).andExpect(status().isCreated());

        // 1. sin clave, con clave incorrecta o con un jwt no se entra
        String ruta = "/api/v1/internal/usuarios/" + U_CAMILA + "/hechos-pago";
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
        mvc.perform(get(ruta).header("X-Internal-Api-Key", "otra")).andExpect(status().isUnauthorized());
        mvc.perform(get(ruta).header("Authorization", camila())).andExpect(status().isUnauthorized());

        // 2. el pagado trae su comprobante y fecha; el pendiente no inventa pago
        String json = mvc.perform(get(ruta).header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(U_CAMILA.toString()))
                .andExpect(jsonPath("$.compromisos", hasSize(2)))
                .andExpect(jsonPath("$.compromisos[0].folio").value("CP-2026-0401"))
                .andExpect(jsonPath("$.compromisos[0].estado").value("PAGADO"))
                .andExpect(jsonPath("$.compromisos[0].pago.numeroComprobante").value("CPR-000001"))
                .andExpect(jsonPath("$.compromisos[0].pago.fechaHoraPago").exists())
                .andExpect(jsonPath("$.compromisos[1].estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.compromisos[1].pago").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        // el cuerpo se lee con el mismo contrato que usa reconocimientos-service
        var hechos = tools.jackson.databind.json.JsonMapper.builder().build()
                .readValue(json, com.pagoflex.contracts.cobranza.RespuestaHechosPago.class);
        assertThat(hechos.compromisos().get(0).pago().numeroComprobante()).isEqualTo("CPR-000001");
        assertThat(hechos.compromisos().get(0).estado())
                .isEqualTo(com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion.PAGADO);

        // 3. una cuenta sin persona es un recurso inexistente
        mvc.perform(get("/api/v1/internal/usuarios/" + UUID.randomUUID() + "/hechos-pago")
                        .header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isNotFound());
        when(usuarios.porUsuario(U_LUIS)).thenReturn(Optional.of(LUIS));
        mvc.perform(get("/api/v1/internal/usuarios/" + U_LUIS + "/hechos-pago").header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(jsonPath("$.compromisos", hasSize(0)));
    }

    @Test
    void despuesDeConfirmarElPagoSePideEvaluarReconocimientos() throws Exception {
        compromiso("CP-2026-0411", CAMILA, "EC-03", "Crédito", 1, 1, 80_000, "2026-10-25", EstadoCompromiso.PENDIENTE);
        String cotizacion = campo(mvc.perform(get("/api/v1/compromisos/me/CP-2026-0411/cotizacion-pago")
                .header("Authorization", camila())), "cotizacionId");

        mvc.perform(pagar("CP-2026-0411", "llave-ev", cotizacion)).andExpect(status().isCreated());

        verify(reconocimientos).evaluar(U_CAMILA, null);
    }

    @Test
    void siReconocimientosFallaElPagoConfirmadoSeMantieneSinDuplicarse() throws Exception {
        compromiso("CP-2026-0412", CAMILA, "EC-03", "Crédito", 1, 1, 80_000, "2026-10-25", EstadoCompromiso.PENDIENTE);
        String cotizacion = campo(mvc.perform(get("/api/v1/compromisos/me/CP-2026-0412/cotizacion-pago")
                .header("Authorization", camila())), "cotizacionId");
        when(reconocimientos.evaluar(any(), any())).thenReturn(false);

        mvc.perform(pagar("CP-2026-0412", "llave-falla", cotizacion)).andExpect(status().isCreated());
        // repetir con la misma llave no cobra ni emite otro comprobante, y reintenta la evaluación
        mvc.perform(pagar("CP-2026-0412", "llave-falla", cotizacion)).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroComprobante").value("CPR-000001"));

        assertThat(pagoRepo.count()).isEqualTo(1);
        assertThat(comprobanteRepo.count()).isEqualTo(1);
        verify(reconocimientos, times(2)).evaluar(U_CAMILA, null);
    }

    // ---------- empresa del ejecutivo ----------
    @Test
    void unAgenteNoRecibePermisosDeEjecutivoNiDeEmpresa() throws Exception {
        UUID agente = UUID.randomUUID();
        when(usuarios.porUsuario(agente)).thenReturn(Optional.of(new PersonaInterna(UUID.randomUUID(), "2222222-2",
                "Agente", "Interno", "a@pagoflex.ejemplo", null, agente, IdentificadoresEmpresa.INTERNA)));

        // 1. el rol AGENTE no entra a rutas de ejecutivo
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", bearer(agente, "a@pagoflex.ejemplo", "AGENTE")))
                .andExpect(status().isForbidden());
        // 2. aunque tuviera el rol, la organización interna no es una empresa cliente
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", bearer(agente, "a@pagoflex.ejemplo", "EJECUTIVO")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/internal/empresas/ejecutivos/" + agente).header("X-Internal-Api-Key", CLAVE_INTERNA))
                .andExpect(status().isNotFound());
    }

    @Test
    void unEjecutivoSoloVeLaEmpresaDeSuPersonaYElTokenNoLaCambia() throws Exception {
        // 1. dos ejecutivos de empresas distintas ven su propia empresa
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", altos()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("EC-01"));
        mvc.perform(get("/api/v1/empresas/me").header("Authorization", credito()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.codigo").value("EC-03"));

        // 2. un compromiso de EC-01 no existe para el ejecutivo de EC-03
        compromiso("CP-2026-0421", LUIS, "EC-01", "Cuota pie", 1, 36, 310_000, "2026-10-30", EstadoCompromiso.PENDIENTE);
        mvc.perform(get("/api/v1/empresas/me/compromisos/CP-2026-0421").header("Authorization", credito()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/empresas/me/compromisos/CP-2026-0421").header("Authorization", altos()))
                .andExpect(status().isOk());
    }

    // --- helpers ---
    private static String altos() {
        return bearer(EJ_ALTOS, "psaavedra@altosmaipo.ejemplo", "EJECUTIVO");
    }

    private static String credito() {
        return bearer(EJ_CREDITO, "chenriquez@creditoandino.ejemplo", "EJECUTIVO");
    }

    private static String camila() {
        return bearer(U_CAMILA, CAMILA.correo(), "USUARIO_FINAL");
    }

    private static String luis() {
        return bearer(U_LUIS, LUIS.correo(), "USUARIO_FINAL");
    }

    private MockHttpServletRequestBuilder pagar(String folio, String llave, String cotizacion) {
        return post("/api/v1/compromisos/me/" + folio + "/pagos")
                .header("Authorization", camila())
                .header("Idempotency-Key", llave)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"medioPago\":\"DEBITO\",\"cotizacionId\":\"" + cotizacion + "\"}");
    }

    private static String campo(ResultActions r, String nombre) throws Exception {
        var m = java.util.regex.Pattern.compile("\"" + nombre + "\":\"([^\"]+)\"")
                .matcher(r.andReturn().getResponse().getContentAsString());
        assertThat(m.find()).isTrue();
        return m.group(1);
    }

    private static String registro(String rut, String concepto, Integer cuota, Integer total, String vence) {
        return """
                {"rut":"%s","concepto":"%s","monto":310000,"fechaVencimiento":"%s","numeroCuota":%s,"totalCuotas":%s}
                """.formatted(rut, concepto, vence, cuota, total);
    }

    private void compromiso(String folio, PersonaInterna p, String empresa, String concepto, Integer cuota,
                            Integer total, long monto, String vence, EstadoCompromiso estado) {
        LocalDateTime carga = LocalDateTime.of(2026, 8, 30, 9, 0);
        Compromiso c = compromisoRepo.save(Compromiso.builder()
                .folio(folio)
                .empresa(empresaRepo.findByCodigo(empresa).orElseThrow())
                .personaId(p.id())
                .personaRut(p.rut())
                .personaNombre(p.nombres() + " " + p.apellidos())
                .concepto(concepto)
                .numeroCuota(cuota)
                .totalCuotas(total)
                .monto(monto)
                .fechaVencimiento(LocalDate.parse(vence))
                .estado(estado)
                .fechaCambioEstado(carga)
                .fechaRegistro(carga)
                .build());
        assertThat(c.getId()).isNotNull();
    }
}
