package com.pagoflex.cobranza.features.compromiso;

import com.pagoflex.cobranza.support.RelojFijo;
import com.pagoflex.cobranza.support.TokensPrueba;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/// Con la carga demo la cobranza de septiembre cuadra con el anexo 9
@SpringBootTest(properties = {
        "pagoflex.demo.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:cobranza-demo;DB_CLOSE_DELAY=-1"
})
@AutoConfigureMockMvc
@Import(RelojFijo.class)
class CargaDemoTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void septemberSummaryMatchesTheCase() throws Exception {
        String paula = TokensPrueba.bearer(UUID.randomUUID(), "psaavedra@altosmaipo.ejemplo", "EJECUTIVO");

        mvc.perform(get("/api/v1/cobranza/resumen").param("mes", "2026-09").header("Authorization", paula))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.empresa").value("Altos del Maipo"))
                .andExpect(jsonPath("$.totalCompromisos").value(120))
                .andExpect(jsonPath("$.pagados").value(104))
                .andExpect(jsonPath("$.anulados").value(2))
                .andExpect(jsonPath("$.tasaCobranza").value(88.1))
                .andExpect(jsonPath("$.recaudado").value(32_476_000));
    }

    @Test
    void demoUserSeesHerCommitments() throws Exception {
        String camila = TokensPrueba.bearer(UUID.randomUUID(), "crojas@correo.ejemplo", "USUARIO_FINAL");

        mvc.perform(get("/api/v1/cobranza/mis-compromisos").header("Authorization", camila))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5));
    }
}
