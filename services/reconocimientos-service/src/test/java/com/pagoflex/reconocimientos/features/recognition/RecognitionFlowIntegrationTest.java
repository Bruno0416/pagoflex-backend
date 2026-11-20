package com.pagoflex.reconocimientos.features.recognition;

import com.pagoflex.reconocimientos.support.IntegrationTest;
import com.pagoflex.reconocimientos.features.recognition.repository.UserRecognitionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecognitionFlowIntegrationTest extends IntegrationTest {
    private static final UUID USER = UUID.fromString("3f1c2b9e-6a1d-4c39-9b55-0d6f6f1f2a10");

    @Autowired
    private UserRecognitionRepository repository;

    @BeforeEach
    void clearGrantsBetweenScenarios() {
        repository.deleteAll();
    }

    // ---------- la consulta publica debe evaluar como lo hacia Room ----------
    @Test
    void getRecognitionsEvaluatesIdempotentlyWithoutAnExtraPublicEndpoint() throws Exception {
        try (var stream = getClass().getResourceAsStream("/contracts/cobranza/hechos-pago.json")) {
            cobranza.respond(200, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        mvc.perform(get("/api/v1/reconocimientos/me").header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/api/v1/reconocimientos/me").header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(post("/api/v1/internal/reconocimientos/evaluar")
                        .header("X-Internal-Api-Key", "test-only-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + USER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otorgados.length()").value(0));
    }

    // ---------- flujo completo ----------
    @Test
    void endToEndSignalPullGrantAndRead() throws Exception {
        // 1. cobranza responde con la fixture canonica del contrato
        try (var stream = getClass().getResourceAsStream("/contracts/cobranza/hechos-pago.json")) {
            cobranza.respond(200, new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
        // 2. la senal interna dispara la consulta y el otorgamiento
        mvc.perform(post("/api/v1/internal/reconocimientos/evaluar")
                        .header("X-Internal-Api-Key", "test-only-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + USER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otorgados.length()").value(2));
        // 3. el usuario ve sus reconocimientos con la fecha de obtencion
        mvc.perform(get("/api/v1/reconocimientos/me").header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.codigo=='TRES_MESES_AL_DIA')].motivo")
                        .value("Pagos al día de julio 2026 a septiembre 2026"))
                .andExpect(jsonPath("$[0].fechaObtencion").value("2026-10-06T09:00:00-03:00"));
        // 4. repetir la senal no otorga nada nuevo
        mvc.perform(post("/api/v1/internal/reconocimientos/evaluar")
                        .header("X-Internal-Api-Key", "test-only-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + USER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otorgados.length()").value(0));
    }
}
