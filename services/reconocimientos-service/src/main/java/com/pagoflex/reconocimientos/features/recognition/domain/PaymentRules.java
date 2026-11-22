package com.pagoflex.reconocimientos.features.recognition.domain;

import com.pagoflex.contracts.cobranza.HechoCompromiso;
import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;

public final class PaymentRules {
    private static final String[] MONTHS = {"enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
            "agosto", "septiembre", "octubre", "noviembre", "diciembre"};
    private static final int STREAK_LENGTH = 3;

    private PaymentRules() {
    }

    // ---------- evaluacion completa ----------
    public static List<RecognitionCandidate> evaluate(List<HechoCompromiso> facts, LocalDate today, ZoneId zone) {
        // 1. reunir los reconocimientos que corresponden segun los hechos
        List<RecognitionCandidate> result = new ArrayList<>();
        firstOnTimePayment(facts, zone).ifPresent(result::add);
        threeMonthStreak(facts, today, zone).ifPresent(result::add);
        return result;
    }

    // ---------- primer pago a tiempo ----------
    public static Optional<RecognitionCandidate> firstOnTimePayment(List<HechoCompromiso> facts, ZoneId zone) {
        // 1. elegir el pago a tiempo mas antiguo; el empate lo resuelve el menor folio
        return facts.stream()
                .filter(fact -> paidOnTime(fact, zone))
                .min(Comparator.<HechoCompromiso, Instant>comparing(fact -> fact.pago().fechaHoraPago().toInstant())
                        .thenComparing(HechoCompromiso::folio))
                .map(fact -> new RecognitionCandidate(RecognitionCode.PRIMER_PAGO_A_TIEMPO,
                        RecognitionCandidate.NO_REFERENCE,
                        "Pago " + fact.folio() + " del "
                                + fact.pago().fechaHoraPago().atZoneSameInstant(zone).toLocalDate()));
    }

    // ---------- tres meses al dia ----------
    public static Optional<RecognitionCandidate> threeMonthStreak(List<HechoCompromiso> facts, LocalDate today,
                                                                  ZoneId zone) {
        // 1. agrupar por mes de vencimiento solo los meses cerrados, sin anulados
        YearMonth current = YearMonth.from(today);
        Map<YearMonth, List<HechoCompromiso>> byMonth = facts.stream()
                .filter(fact -> fact.estado() != EstadoCompromisoEvaluacion.ANULADO)
                .filter(fact -> YearMonth.from(fact.fechaVencimiento()).isBefore(current))
                .collect(Collectors.groupingBy(fact -> YearMonth.from(fact.fechaVencimiento()),
                        TreeMap::new, Collectors.toList()));

        // 2. recorrer los meses en orden manteniendo la corrida consecutiva
        List<YearMonth> run = new ArrayList<>();
        for (Map.Entry<YearMonth, List<HechoCompromiso>> entry : byMonth.entrySet()) {
            YearMonth month = entry.getKey();
            if (!entry.getValue().stream().allMatch(fact -> paidOnTime(fact, zone))) {
                run.clear();
                continue;
            }
            if (run.isEmpty() || !run.get(run.size() - 1).equals(month.minusMonths(1))) {
                run.clear();
            }
            run.add(month);
            // 3. otorgar con la primera ventana completa
            if (run.size() == STREAK_LENGTH) {
                return Optional.of(new RecognitionCandidate(RecognitionCode.TRES_MESES_AL_DIA,
                        RecognitionCandidate.NO_REFERENCE,
                        "Pagos al día de " + label(run.get(0)) + " a " + label(run.get(run.size() - 1))));
            }
        }
        return Optional.empty();
    }

    // --- helper ---
    private static boolean paidOnTime(HechoCompromiso fact, ZoneId zone) {
        // 1. solo cuenta un pago confirmado cuya fecha local no supera el vencimiento
        if (fact.estado() != EstadoCompromisoEvaluacion.PAGADO || fact.pago() == null) return false;
        LocalDate paidOn = fact.pago().fechaHoraPago().atZoneSameInstant(zone).toLocalDate();
        return !paidOn.isAfter(fact.fechaVencimiento());
    }

    // --- helper ---
    private static String label(YearMonth month) {
        return MONTHS[month.getMonthValue() - 1] + " " + month.getYear();
    }
}
