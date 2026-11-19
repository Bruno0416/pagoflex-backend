package com.pagoflex.reconocimientos.features.recognition.domain;

import com.pagoflex.contracts.cobranza.CommitmentFact;
import com.pagoflex.contracts.cobranza.CommitmentStatus;
import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import static com.pagoflex.reconocimientos.support.Facts.paid;
import static com.pagoflex.reconocimientos.support.Facts.withStatus;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentRulesTest {
    private static final ZoneId SANTIAGO = ZoneId.of("America/Santiago");
    private static final LocalDate TODAY = LocalDate.parse("2026-10-06");

    // ---------- primer pago a tiempo ----------
    @Test
    void firstOnTimePaymentPicksEarliestPaidOnTime() {
        var later = paid("CP-2", "2026-08-05", "2026-08-04T10:00:00-04:00");
        var earlier = paid("CP-1", "2026-07-05", "2026-07-01T10:00:00-04:00");
        var result = PaymentRules.firstOnTimePayment(List.of(later, earlier), SANTIAGO);
        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo(RecognitionCode.PRIMER_PAGO_A_TIEMPO);
        assertThat(result.get().reference()).isEqualTo("-");
        assertThat(result.get().reason()).contains("CP-1");
    }

    @Test
    void latePaymentIsNotFirstOnTime() {
        var late = paid("CP-1", "2026-07-05", "2026-07-06T10:00:00-04:00");
        assertThat(PaymentRules.firstOnTimePayment(List.of(late), SANTIAGO)).isEmpty();
    }

    @Test
    void annulledAndInReviewAreIgnored() {
        var facts = List.of(withStatus("CP-1", "2026-07-05", CommitmentStatus.ANULADO),
                withStatus("CP-2", "2026-07-05", CommitmentStatus.EN_REVISION),
                withStatus("CP-3", "2026-07-05", CommitmentStatus.VENCIDO),
                withStatus("CP-4", "2026-12-05", CommitmentStatus.PENDIENTE));
        assertThat(PaymentRules.firstOnTimePayment(facts, SANTIAGO)).isEmpty();
    }

    @Test
    void paymentAt2359LocalOnDueDateIsOnTime() {
        var fact = paid("CP-1", "2026-07-05", "2026-07-05T23:59:00-04:00");
        assertThat(PaymentRules.firstOnTimePayment(List.of(fact), SANTIAGO)).isPresent();
    }

    @Test
    void paymentNextDayLocalIsLate() {
        // 06-jul 04:30 utc = 06-jul 00:30 en santiago
        var fact = paid("CP-1", "2026-07-05", "2026-07-06T04:30:00Z");
        assertThat(PaymentRules.firstOnTimePayment(List.of(fact), SANTIAGO)).isEmpty();
    }

    @Test
    void utcNextDayButSantiagoSameDayIsOnTime() {
        // 06-jul 02:30 utc = 05-jul 22:30 en santiago
        var fact = paid("CP-1", "2026-07-05", "2026-07-06T02:30:00Z");
        assertThat(PaymentRules.firstOnTimePayment(List.of(fact), SANTIAGO)).isPresent();
    }

    @Test
    void sameInstantTieBreaksByLowestFolio() {
        var b = paid("CP-B", "2026-07-05", "2026-07-01T10:00:00-04:00");
        var a = paid("CP-A", "2026-07-05", "2026-07-01T10:00:00-04:00");
        assertThat(PaymentRules.firstOnTimePayment(List.of(b, a), SANTIAGO).orElseThrow().reason())
                .contains("CP-A");
    }

    @Test
    void emptyHistoryGrantsNothing() {
        assertThat(PaymentRules.evaluate(List.of(), TODAY, SANTIAGO)).isEmpty();
    }

    // ---------- racha de tres meses ----------
    @Test
    void threeConsecutiveClosedMonthsGrantStreak() {
        var result = PaymentRules.threeMonthStreak(monthsPaidOnTime("2026-05", "2026-06", "2026-07"), TODAY, SANTIAGO);
        assertThat(result).isPresent();
        assertThat(result.get().code()).isEqualTo(RecognitionCode.TRES_MESES_AL_DIA);
        assertThat(result.get().reference()).isEqualTo("-");
    }

    @Test
    void firstWindowOfThreeIsReported() {
        var facts = monthsPaidOnTime("2026-04", "2026-05", "2026-06", "2026-07", "2026-08", "2026-09");
        var result = PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO).orElseThrow();
        assertThat(result.reason()).isEqualTo("Pagos al día de abril 2026 a junio 2026");
    }

    @Test
    void currentMonthIsNotCounted() {
        // octubre es el mes actual: solo dos meses cerrados consecutivos
        var facts = monthsPaidOnTime("2026-08", "2026-09", "2026-10");
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void streakAcrossYearChange() {
        var facts = monthsPaidOnTime("2025-11", "2025-12", "2026-01");
        var result = PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO).orElseThrow();
        assertThat(result.reason()).isEqualTo("Pagos al día de noviembre 2025 a enero 2026");
    }

    @Test
    void gapMonthBreaksStreak() {
        var facts = monthsPaidOnTime("2026-04", "2026-05", "2026-07");
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void separatedMonthsDoNotGrant() {
        var facts = monthsPaidOnTime("2026-01", "2026-03", "2026-05");
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void annulledOnlyMonthIsGap() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-04", "2026-06", "2026-07"));
        facts.add(withStatus("CP-ANU", "2026-05-05", CommitmentStatus.ANULADO));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void annulledCommitmentDoesNotInvalidateAMonthWithOtherPaidOnTime() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-05", "2026-06", "2026-07"));
        facts.add(withStatus("CP-ANU", "2026-06-20", CommitmentStatus.ANULADO));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isPresent();
    }

    @Test
    void oneLateInstallmentInvalidatesMonth() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-05", "2026-06", "2026-07"));
        facts.add(paid("CP-TARDE", "2026-06-20", "2026-06-25T10:00:00-04:00"));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void multipleInstallmentsAllOnTimeKeepMonthValid() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-05", "2026-06", "2026-07"));
        facts.add(paid("CP-EXTRA", "2026-06-20", "2026-06-19T10:00:00-04:00"));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isPresent();
    }

    @Test
    void inReviewInvalidatesMonth() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-05", "2026-06", "2026-07"));
        facts.add(withStatus("CP-REV", "2026-06-20", CommitmentStatus.EN_REVISION));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void unpaidOverdueInvalidatesMonth() {
        var facts = new java.util.ArrayList<>(monthsPaidOnTime("2026-05", "2026-06", "2026-07"));
        facts.add(withStatus("CP-VENC", "2026-07-20", CommitmentStatus.VENCIDO));
        assertThat(PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO)).isEmpty();
    }

    @Test
    void lateMonthRestartsTheRun() {
        // may ok, jun tarde, jul-ago-sep ok: la racha empieza en julio
        var facts = new java.util.ArrayList<CommitmentFact>();
        facts.addAll(monthsPaidOnTime("2026-05", "2026-07", "2026-08", "2026-09"));
        facts.add(paid("CP-JUN", "2026-06-05", "2026-06-10T10:00:00-04:00"));
        var result = PaymentRules.threeMonthStreak(facts, TODAY, SANTIAGO).orElseThrow();
        assertThat(result.reason()).isEqualTo("Pagos al día de julio 2026 a septiembre 2026");
    }

    // ---------- evaluacion combinada y monto ----------
    @Test
    void evaluateReturnsBothWhenFactsQualify() {
        var facts = monthsPaidOnTime("2026-05", "2026-06", "2026-07");
        assertThat(PaymentRules.evaluate(facts, TODAY, SANTIAGO)).extracting(RecognitionCandidate::code)
                .containsExactlyInAnyOrder(RecognitionCode.PRIMER_PAGO_A_TIEMPO, RecognitionCode.TRES_MESES_AL_DIA);
    }

    @Test
    void amountIsNeverConsidered() {
        // rn-16: el contrato no transporta montos, por lo que no pueden influir
        assertThat(Arrays.stream(CommitmentFact.class.getRecordComponents()).map(c -> c.getName()))
                .noneMatch(name -> name.toLowerCase().contains("monto"));
    }

    // --- helper ---
    private static List<CommitmentFact> monthsPaidOnTime(String... months) {
        // 1. un compromiso por mes, pagado un dia antes del vencimiento (dia 5)
        return Arrays.stream(months)
                .map(month -> paid("CP-" + month, month + "-05", month + "-04T10:00:00-04:00"))
                .toList();
    }
}
