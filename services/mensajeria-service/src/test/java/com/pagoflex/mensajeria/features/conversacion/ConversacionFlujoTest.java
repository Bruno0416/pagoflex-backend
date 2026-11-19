package com.pagoflex.mensajeria.features.conversacion;

import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.mensajeria.core.client.CobranzaServiceClient;
import com.pagoflex.mensajeria.core.client.SoporteServiceClient;
import com.pagoflex.mensajeria.features.conversacion.repository.ConversacionRepository;
import com.pagoflex.mensajeria.features.conversacion.repository.LecturaRepository;
import com.pagoflex.mensajeria.features.conversacion.repository.MensajeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.Optional;
import java.util.UUID;

import static com.pagoflex.mensajeria.support.TokensPrueba.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ConversacionFlujoTest {
    private static final UUID EMPRESA = UUID.randomUUID();
    private static final UUID OTRA_EMPRESA = UUID.randomUUID();
    private static final UUID AGENTE = UUID.randomUUID();
    private static final UUID OTRO_AGENTE = UUID.randomUUID();
    private static final UUID EJECUTIVO = UUID.randomUUID();
    private static final UUID EJECUTIVO_AJENO = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private MensajeRepository mensajeRepo;
    @Autowired
    private LecturaRepository lecturaRepo;
    @Autowired
    private ConversacionRepository conversacionRepo;

    @MockitoBean
    private SoporteServiceClient soporte;
    @MockitoBean
    private CobranzaServiceClient cobranza;

    @BeforeEach
    void preparar() {
        lecturaRepo.deleteAll();
        mensajeRepo.deleteAll();
        conversacionRepo.deleteAll();
        reporte("REP-0013", "EN_ATENCION", AGENTE);
        when(cobranza.empresaDeEjecutivo(EJECUTIVO)).thenReturn(Optional.of(new EmpresaInterna(EMPRESA, "EC-01", "Altos del Maipo")));
        when(cobranza.empresaDeEjecutivo(EJECUTIVO_AJENO)).thenReturn(Optional.of(new EmpresaInterna(OTRA_EMPRESA, "EC-03", "Crédito Andino")));
    }

    // ---------- permisos ----------
    @Test
    void soloConversanElAgenteAsignadoYElEjecutivoDeLaEmpresa() throws Exception {
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013")
                        .header("Authorization", bearer(UUID.randomUUID(), "u@correo.ejemplo", "USUARIO_FINAL")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", otroAgente()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", ejecutivoAjeno()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-9999").header("Authorization", agente()))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", agente()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.soloLectura").value(false));
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", ejecutivo()))
                .andExpect(status().isOk());
    }

    @Test
    void sinAgenteAsignadoNoHayConversacion() throws Exception {
        reporte("REP-0012", "ABIERTO", null);
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0012").header("Authorization", ejecutivo()))
                .andExpect(status().isConflict());
    }

    // ---------- mensajes ----------
    @Test
    void enviaYLeeEnOrdenSinDuplicarReintentos() throws Exception {
        mvc.perform(enviar(agente(), "Hola, ¿pueden revisar el monto del cobro?", "m-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secuencia").value(1))
                .andExpect(jsonPath("$.rol").value("AGENTE"));
        // la app reintenta el mismo mensaje: no se duplica
        mvc.perform(enviar(agente(), "Hola, ¿pueden revisar el monto del cobro?", "m-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.secuencia").value(1));
        mvc.perform(enviar(ejecutivo(), "Sí, el monto incluye el recargo de septiembre.", "m-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.secuencia").value(2))
                .andExpect(jsonPath("$.rol").value("EJECUTIVO"));

        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/mensajes").header("Authorization", ejecutivo()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mensajes", hasSize(2)))
                .andExpect(jsonPath("$.mensajes[0].autor").value("dortiz@pagoflex.ejemplo"))
                .andExpect(jsonPath("$.mensajes[1].secuencia").value(2))
                .andExpect(jsonPath("$.cursorAnterior").value(nullValue()));
        assertThat(mensajeRepo.count()).isEqualTo(2);
    }

    @Test
    void historialPaginadoPorCursor() throws Exception {
        for (int i = 1; i <= 5; i++) mvc.perform(enviar(agente(), "mensaje " + i, "m-" + i));

        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/mensajes?limit=2").header("Authorization", agente()))
                .andExpect(jsonPath("$.mensajes", hasSize(2)))
                .andExpect(jsonPath("$.mensajes[0].secuencia").value(4))
                .andExpect(jsonPath("$.cursorAnterior").value(4));
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/mensajes?limit=2&cursor=4").header("Authorization", agente()))
                .andExpect(jsonPath("$.mensajes[0].secuencia").value(2))
                .andExpect(jsonPath("$.mensajes[1].secuencia").value(3));
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/mensajes?limit=2&cursor=2").header("Authorization", agente()))
                .andExpect(jsonPath("$.mensajes", hasSize(1)))
                .andExpect(jsonPath("$.cursorAnterior").value(nullValue()));
    }

    @Test
    void validaElTextoYCierraAlResolver() throws Exception {
        mvc.perform(enviar(agente(), "   ", "m-1")).andExpect(status().isBadRequest());
        mvc.perform(enviar(agente(), "x".repeat(1001), "m-2")).andExpect(status().isBadRequest());
        mvc.perform(enviar(agente(), "Antes de resolver", "m-3")).andExpect(status().isCreated());

        // resuelto: se puede leer pero no escribir
        reporte("REP-0013", "RESUELTO", AGENTE);
        mvc.perform(enviar(ejecutivo(), "¿Ya está?", "m-4")).andExpect(status().isConflict());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", ejecutivo()))
                .andExpect(jsonPath("$.soloLectura").value(true));
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/mensajes").header("Authorization", ejecutivo()))
                .andExpect(jsonPath("$.mensajes", hasSize(1)));
        assertThat(conversacionRepo.findByReporteNumero("REP-0013").orElseThrow().getFechaCierre()).isNotNull();
    }

    @Test
    void lecturaSoloAvanza() throws Exception {
        mvc.perform(enviar(agente(), "uno", "m-1"));
        mvc.perform(enviar(agente(), "dos", "m-2"));
        mvc.perform(put("/api/v1/conversaciones/reportes/REP-0013/lecturas").header("Authorization", ejecutivo())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ultimaSecuencia\":9}")).andExpect(status().isNoContent());
        mvc.perform(put("/api/v1/conversaciones/reportes/REP-0013/lecturas").header("Authorization", ejecutivo())
                .contentType(MediaType.APPLICATION_JSON).content("{\"ultimaSecuencia\":1}")).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013").header("Authorization", ejecutivo()))
                .andExpect(jsonPath("$.ultimaSecuenciaLeida").value(2));
    }

    // ---------- tiempo real ----------
    @Test
    void sseEntregaLosNuevosYRecuperaLosPerdidos() throws Exception {
        mvc.perform(enviar(agente(), "primero", "m-1"));
        mvc.perform(enviar(agente(), "segundo", "m-2"));

        // 1. al reconectar con Last-Event-ID llegan solo los posteriores
        MvcResult reconexion = mvc.perform(get("/api/v1/conversaciones/reportes/REP-0013/eventos")
                        .header("Authorization", ejecutivo()).header("Last-Event-ID", "1"))
                .andExpect(request().asyncStarted())
                .andReturn();
        String recibido = reconexion.getResponse().getContentAsString();
        assertThat(recibido).contains("event:message").contains("id:2").contains("segundo").doesNotContain("primero");

        // 2. un mensaje nuevo llega en vivo a la conexión abierta
        mvc.perform(enviar(agente(), "tercero en vivo", "m-3")).andExpect(status().isCreated());
        assertThat(reconexion.getResponse().getContentAsString()).contains("id:3").contains("tercero en vivo");
    }

    // --- helpers ---
    private void reporte(String numero, String estado, UUID agente) {
        when(soporte.participantes(numero)).thenReturn(Optional.of(
                new SoporteServiceClient.Participantes(numero, "CP-2026-0106", estado, EMPRESA, agente, UUID.randomUUID())));
    }

    private static String agente() {
        return bearer(AGENTE, "dortiz@pagoflex.ejemplo", "AGENTE");
    }

    private static String otroAgente() {
        return bearer(OTRO_AGENTE, "alagos@pagoflex.ejemplo", "AGENTE");
    }

    private static String ejecutivo() {
        return bearer(EJECUTIVO, "psaavedra@altosmaipo.ejemplo", "EJECUTIVO");
    }

    private static String ejecutivoAjeno() {
        return bearer(EJECUTIVO_AJENO, "chenriquez@creditoandino.ejemplo", "EJECUTIVO");
    }

    private static MockHttpServletRequestBuilder enviar(String token, String texto, String clientId) {
        return post("/api/v1/conversaciones/reportes/REP-0013/mensajes")
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"" + texto + "\",\"clientMessageId\":\"" + clientId + "\"}");
    }
}
