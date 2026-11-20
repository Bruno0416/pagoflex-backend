package com.pagoflex.reconocimientos.contracts;

import com.pagoflex.contracts.cobranza.CommitmentStatus;
import com.pagoflex.contracts.cobranza.PaymentFactsResponse;
import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsRequest;
import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsResponse;
import com.pagoflex.reconocimientos.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CobranzaContractJsonTest extends IntegrationTest {
    @Autowired
    JsonMapper mapper;

    // ---------- compatibilidad del contrato json ----------
    @Test
    void canonicalFixtureDeserializesIntoSharedDto() throws IOException {
        try (var stream = getClass().getResourceAsStream("/contracts/cobranza/hechos-pago.json")) {
            var facts = mapper.readValue(stream, PaymentFactsResponse.class);
            assertThat(facts.idUsuario()).isEqualTo(UUID.fromString("3f1c2b9e-6a1d-4c39-9b55-0d6f6f1f2a10"));
            assertThat(facts.compromisos()).extracting(c -> c.estado())
                    .containsExactly(CommitmentStatus.PAGADO, CommitmentStatus.PAGADO, CommitmentStatus.ANULADO,
                            CommitmentStatus.PAGADO, CommitmentStatus.PENDIENTE);
            assertThat(facts.compromisos().get(0).pago().fechaHoraPago().toInstant())
                    .isEqualTo(OffsetDateTime.parse("2026-07-04T18:20:00-04:00").toInstant());
        }
    }

    @Test
    void evaluateRequestSerializesSpanishFields() {
        UUID user = UUID.randomUUID();
        UUID event = UUID.randomUUID();
        String json = mapper.writeValueAsString(new EvaluateRecognitionsRequest(user, event));
        assertThat(json).contains("\"idUsuario\":\"" + user + "\"").contains("\"idEvento\":\"" + event + "\"");
    }

    @Test
    void evaluateResponseSerializesSpanishFields() {
        String json = mapper.writeValueAsString(new EvaluateRecognitionsResponse(UUID.randomUUID(),
                List.of("PRIMER_PAGO_A_TIEMPO"), OffsetDateTime.parse("2026-10-06T09:00:00-03:00")));
        assertThat(json).contains("\"otorgados\":[\"PRIMER_PAGO_A_TIEMPO\"]").contains("\"evaluadoEn\"");
    }

    @Test
    void unknownFieldsAreIgnored() {
        var request = mapper.readValue("{\"idUsuario\":\"" + UUID.randomUUID() + "\",\"extra\":1}",
                EvaluateRecognitionsRequest.class);
        assertThat(request.idEvento()).isNull();
    }
}
