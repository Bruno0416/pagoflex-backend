package com.pagoflex.reconocimientos.features.recognition;

import com.pagoflex.contracts.cobranza.HechoCompromiso;
import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.reconocimientos.features.recognition.repository.UserRecognitionRepository;
import com.pagoflex.reconocimientos.support.Facts;
import com.pagoflex.reconocimientos.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.pagoflex.reconocimientos.support.Facts.paid;
import static com.pagoflex.reconocimientos.support.Facts.withStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RecognitionM3RegressionTest extends IntegrationTest {
    private static final UUID USER = UUID.fromString("4ea45b64-e2ea-41cb-9cc3-cfb61059a1a7");

    @Autowired
    private UserRecognitionRepository repository;

    @BeforeEach
    void clearPreviousGrants() {
        repository.deleteAll();
    }

    @Test
    void twelveInstallmentsWithOnlyTwoPaidDoNotGrantStreak() throws Exception {
        var facts = new ArrayList<HechoCompromiso>();
        facts.add(paid("PLAN-01", "2026-08-05", "2026-08-04T10:00:00-04:00"));
        facts.add(paid("PLAN-02", "2026-09-05", "2026-09-04T10:00:00-04:00"));
        for (int quota = 3; quota <= 12; quota++) {
            YearMonth month = YearMonth.of(2026, 8).plusMonths(quota - 1);
            facts.add(withStatus("PLAN-%02d".formatted(quota),
                    month.atDay(5).toString(), EstadoCompromisoEvaluacion.PENDIENTE));
        }
        assertThat(facts).hasSize(12);
        cobranza.respond(200, Facts.body(USER, facts.toArray(HechoCompromiso[]::new)));

        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("PRIMER_PAGO_A_TIEMPO"));
        assertThat(repository.findByUserIdOrderByObtainedAtDescIdDesc(USER)).hasSize(1);
    }

    @Test
    void threePaidMonthsKeepStreakDespiteNineFutureInstallments() throws Exception {
        var facts = new ArrayList<HechoCompromiso>();
        facts.add(paid("PLAN-01", "2026-07-05", "2026-07-04T10:00:00-04:00"));
        facts.add(paid("PLAN-02", "2026-08-05", "2026-08-04T10:00:00-04:00"));
        facts.add(paid("PLAN-03", "2026-09-05", "2026-09-04T10:00:00-04:00"));
        for (int quota = 4; quota <= 12; quota++) {
            YearMonth month = YearMonth.of(2026, 7).plusMonths(quota - 1);
            facts.add(withStatus("PLAN-%02d".formatted(quota),
                    month.atDay(5).toString(), EstadoCompromisoEvaluacion.PENDIENTE));
        }
        cobranza.respond(200, Facts.body(USER, facts.toArray(HechoCompromiso[]::new)));

        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.codigo=='TRES_MESES_AL_DIA')].motivo")
                        .value("Pagos al día de julio 2026 a septiembre 2026"));
    }

    @Test
    void overdueDebtFromAnotherCompanyBlocksMonthlyStreak() throws Exception {
        cobranza.respond(200, Facts.body(USER,
                paid("EMPRESA-A-JUL", "2026-07-05", "2026-07-04T10:00:00-04:00"),
                paid("EMPRESA-A-AGO", "2026-08-05", "2026-08-04T10:00:00-04:00"),
                paid("EMPRESA-A-SEP", "2026-09-05", "2026-09-04T10:00:00-04:00"),
                withStatus("EMPRESA-B-AGO", "2026-08-20", EstadoCompromisoEvaluacion.VENCIDO)));

        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].codigo").value("PRIMER_PAGO_A_TIEMPO"));
    }

    @Test
    void paymentAtSantiagoMidnightUsesLocalCalendarDate() throws Exception {
        cobranza.respond(200, Facts.body(USER,
                paid("TARDE", "2026-09-30", "2026-10-01T03:01:00Z")));
        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        cobranza.respond(200, Facts.body(USER,
                paid("A-TIEMPO", "2026-09-30", "2026-10-01T02:59:00Z")));
        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void cobranzaOutageKeepsGrantsAndRecoveryDoesNotDuplicateThem() throws Exception {
        cobranza.respond(503, "{\"error\":\"temporal\"}");
        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        String recovered = Facts.body(USER,
                paid("JUL", "2026-07-05", "2026-07-04T10:00:00-04:00"),
                paid("AGO", "2026-08-05", "2026-08-04T10:00:00-04:00"),
                paid("SEP", "2026-09-05", "2026-09-04T10:00:00-04:00"));
        cobranza.respond(200, recovered);
        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        cobranza.respond(503, "{\"error\":\"temporal\"}");
        mvc.perform(get("/api/v1/reconocimientos/me")
                        .header("Authorization", token(USER, UF)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        cobranza.respond(200, recovered);
        mvc.perform(post("/api/v1/internal/reconocimientos/evaluar")
                        .header("X-Internal-Api-Key", "test-only-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idUsuario\":\"" + USER + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otorgados.length()").value(0));
        assertThat(repository.findByUserIdOrderByObtainedAtDescIdDesc(USER)).hasSize(2);
    }
}
