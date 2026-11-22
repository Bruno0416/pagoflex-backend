package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;
import com.pagoflex.reconocimientos.features.recognition.entity.UserRecognition;
import com.pagoflex.reconocimientos.features.recognition.repository.UserRecognitionRepository;
import com.pagoflex.reconocimientos.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecognitionControllerTest extends IntegrationTest {
    @Autowired
    UserRecognitionRepository repository;

    // ---------- mis reconocimientos ----------
    @Test
    void meReturnsOnlyCurrentUserGrantsNewestFirst() throws Exception {
        UUID me = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        repository.save(new UserRecognition(me, RecognitionCode.PRIMER_PAGO_A_TIEMPO, "-", "Pago CP-1",
                Instant.parse("2026-08-01T12:00:00Z")));
        repository.save(new UserRecognition(me, RecognitionCode.TRES_MESES_AL_DIA, "-",
                "Pagos al día", Instant.parse("2026-10-01T12:00:00Z")));
        repository.save(new UserRecognition(other, RecognitionCode.TRES_MESES_AL_DIA, "-", "ajeno",
                Instant.parse("2026-09-01T12:00:00Z")));

        cobranza.respond(503, "{}"); // una caida de cobranza no oculta reconocimientos guardados
        mvc.perform(get("/api/v1/reconocimientos/me").header("Authorization", token(me, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].codigo").value("TRES_MESES_AL_DIA"))
                .andExpect(jsonPath("$[0].fechaObtencion").value("2026-10-01T09:00:00-03:00"))
                .andExpect(jsonPath("$[1].codigo").value("PRIMER_PAGO_A_TIEMPO"))
                .andExpect(jsonPath("$[1].motivo").value("Pago CP-1"))
                .andExpect(jsonPath("$[1].nombre").value("Primer pago a tiempo"));
    }

    @Test
    void meEmptyReturnsEmptyArray() throws Exception {
        mvc.perform(get("/api/v1/reconocimientos/me").header("Authorization", token(UUID.randomUUID(), UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
