package com.pagoflex.soporte.features.reporte;

import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.support.RelojFijo;
import com.pagoflex.soporte.support.TokensPrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Con la carga demo están los reportes del anexo 9
@SpringBootTest(properties = {
        "pagoflex.demo.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:soporte-demo;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(RelojFijo.class)
class CargaDemoTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CobranzaServiceClient cobranza;

    @Test
    void inboxHasTheCaseReports() throws Exception {
        String agente = TokensPrueba.bearer(UUID.randomUUID(), "alagos@pagoflex.ejemplo", "AGENTE");

        mvc.perform(get("/api/v1/soporte/bandeja").header("Authorization", agente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
        mvc.perform(get("/api/v1/soporte/bandeja/REP-0011").header("Authorization", agente))
                .andExpect(jsonPath("$.estado").value("EN_GESTION"))
                .andExpect(jsonPath("$.agente").value("dortiz@pagoflex.ejemplo"));

        String luis = TokensPrueba.bearer(UUID.randomUUID(), "lmunoz@correo.ejemplo", "USUARIO_FINAL");
        mvc.perform(get("/api/v1/soporte/reportes/mios").header("Authorization", luis))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numero").value("REP-0012"));
    }
}
