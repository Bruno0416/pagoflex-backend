package com.pagoflex.soporte.features.reporte;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.client.UsuariosServiceClient;
import com.pagoflex.soporte.features.reporte.data.CargaDemo;
import com.pagoflex.soporte.support.RelojFijo;
import com.pagoflex.soporte.support.TokensPrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Con la carga demo están los reportes del anexo 9, con los uuid que entregan usuarios y cobranza
@SpringBootTest(properties = {
        "pagoflex.demo.enabled=true",
        "pagoflex.demo.reintento-ms=60000",
        "spring.datasource.url=jdbc:h2:mem:soporte-demo;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(RelojFijo.class)
class CargaDemoTest {
    private static final UUID EMPRESA_MAIPO = UUID.randomUUID();
    private static final UUID EMPRESA_OTRA = UUID.randomUUID();

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CargaDemo carga;

    @MockitoBean
    private CobranzaServiceClient cobranza;
    @MockitoBean
    private UsuariosServiceClient usuarios;

    private final UUID luis = UUID.randomUUID();
    private final UUID andres = UUID.randomUUID();
    private final UUID daniela = UUID.randomUUID();

    @Test
    void inboxHasTheCaseReports() throws Exception {
        // 1. sin usuarios todavía no carga
        assertThat(carga.intentarCarga()).isFalse();

        // 2. cuando usuarios y cobranza responden queda lista
        persona("hpizarro@correo.ejemplo", UUID.randomUUID());
        persona("msoto@correo.ejemplo", UUID.randomUUID());
        persona("lmunoz@correo.ejemplo", luis);
        persona("alagos@pagoflex.ejemplo", andres);
        persona("dortiz@pagoflex.ejemplo", daniela);
        cobro("CP-2026-0111", EMPRESA_OTRA, "ANULADO");
        cobro("CP-2026-0109", EMPRESA_OTRA, "EN_REVISION");
        cobro("CP-2026-0106", EMPRESA_MAIPO, "EN_REVISION");
        assertThat(carga.intentarCarga()).isTrue();
        assertThat(carga.intentarCarga()).isTrue();

        // 3. daniela ve el abierto y el suyo en atención, no el resuelto de andrés
        String tokenDaniela = TokensPrueba.bearer(daniela, "dortiz@pagoflex.ejemplo", "AGENTE");
        mvc.perform(get("/api/v1/reportes").header("Authorization", tokenDaniela))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(2))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0011"))
                .andExpect(jsonPath("$.contenido[1].numero").value("REP-0012"));
        mvc.perform(get("/api/v1/reportes/REP-0011").header("Authorization", tokenDaniela))
                .andExpect(jsonPath("$.estado").value("EN_ATENCION"))
                .andExpect(jsonPath("$.agente").value("dortiz@pagoflex.ejemplo"))
                .andExpect(jsonPath("$.historial", hasSize(3)));

        String tokenAndres = TokensPrueba.bearer(andres, "alagos@pagoflex.ejemplo", "AGENTE");
        mvc.perform(get("/api/v1/reportes/REP-0009").header("Authorization", tokenAndres))
                .andExpect(jsonPath("$.estado").value("RESUELTO"))
                .andExpect(jsonPath("$.historial", hasSize(4)));

        // 4. luis ve su reporte abierto
        String tokenLuis = TokensPrueba.bearer(luis, "lmunoz@correo.ejemplo", "USUARIO_FINAL");
        mvc.perform(get("/api/v1/reportes/me").header("Authorization", tokenLuis))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].numero").value("REP-0012"))
                .andExpect(jsonPath("$.contenido[0].estado").value("ABIERTO"));
    }

    // --- helpers ---
    private void persona(String correo, UUID usuarioId) {
        when(usuarios.porCorreo(correo)).thenReturn(
                new PersonaInterna(UUID.randomUUID(), "11.111.111-1", "Nombre", "Apellido", correo, null, usuarioId));
    }

    private void cobro(String folio, UUID empresaId, String estado) {
        when(cobranza.reportabilidad(org.mockito.ArgumentMatchers.eq(folio), isNull())).thenReturn(
                new CompromisoInterno(folio, UUID.randomUUID(), empresaId, "Empresa", "Concepto", 1, 3, 100_000,
                        LocalDate.of(2026, 9, 30), estado, "11.111.111-1", "Nombre Apellido", "x@correo.ejemplo",
                        null, false, null));
    }
}
