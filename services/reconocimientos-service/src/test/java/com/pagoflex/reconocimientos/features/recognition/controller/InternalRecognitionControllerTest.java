package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.reconocimientos.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static com.pagoflex.reconocimientos.support.Facts.body;
import static com.pagoflex.reconocimientos.support.Facts.paid;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalRecognitionControllerTest extends IntegrationTest {
    private static final String PATH = "/api/v1/internal/reconocimientos/evaluar";
    private static final String KEY = "test-only-internal-api-key";

    // ---------- validacion ----------
    @Test
    void missingIdUsuarioReturns400() throws Exception {
        mvc.perform(post(PATH).header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.idUsuario").exists());
        assertThat(cobranza.calls.get()).isZero();
    }

    @Test
    void malformedUuidReturns400() throws Exception {
        mvc.perform(post(PATH).header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"idUsuario\":\"no-uuid\"}"))
                .andExpect(status().isBadRequest());
    }

    // ---------- evaluacion ----------
    @Test
    void returnsGrantedCodes() throws Exception {
        UUID user = UUID.randomUUID();
        cobranza.respond(200, body(user, paid("CP-1", "2026-07-05", "2026-07-04T10:00:00-04:00")));
        mvc.perform(post(PATH).header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + user + "\",\"idEvento\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idUsuario").value(user.toString()))
                .andExpect(jsonPath("$.otorgados[0]").value("PRIMER_PAGO_A_TIEMPO"))
                .andExpect(jsonPath("$.evaluadoEn").exists());
        assertThat(cobranza.receivedKey).isEqualTo(KEY);
    }

    @Test
    void cobranzaDownReturns503() throws Exception {
        cobranza.respond(500, "{}");
        mvc.perform(post(PATH).header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value("Cobranza no está disponible; reintenta más tarde"));
    }

    @Test
    void inconsistentCobranzaDataReturns502() throws Exception {
        cobranza.respond(200, body(UUID.randomUUID()));
        mvc.perform(post(PATH).header("X-Internal-Api-Key", KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isBadGateway());
    }
}
