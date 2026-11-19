package com.pagoflex.reconocimientos.support;

import com.pagoflex.contracts.cobranza.CommitmentFact;
import com.pagoflex.contracts.cobranza.CommitmentStatus;
import com.pagoflex.contracts.cobranza.PaymentFact;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Facts {
    private Facts() {
    }

    public static CommitmentFact paid(String folio, String due, String paidAt) {
        return new CommitmentFact(UUID.randomUUID(), folio, LocalDate.parse(due), CommitmentStatus.PAGADO,
                new PaymentFact("CMP-" + folio, OffsetDateTime.parse(paidAt)));
    }

    public static CommitmentFact withStatus(String folio, String due, CommitmentStatus status) {
        return new CommitmentFact(UUID.randomUUID(), folio, LocalDate.parse(due), status, null);
    }

    // ---------- cuerpo json del contrato ----------
    public static String body(UUID userId, CommitmentFact... facts) {
        // 1. serializar cada hecho con los nombres del contrato
        String items = Arrays.stream(facts).map(Facts::json).collect(Collectors.joining(","));
        return "{\"idUsuario\":\"" + userId + "\",\"generadoEn\":\"2026-10-06T09:00:00-03:00\",\"compromisos\":["
                + items + "]}";
    }

    // --- helper ---
    private static String json(CommitmentFact fact) {
        String payment = fact.pago() == null ? "null" : "{\"numeroComprobante\":\"" + fact.pago().numeroComprobante()
                + "\",\"fechaHoraPago\":\"" + fact.pago().fechaHoraPago() + "\"}";
        return "{\"idCompromiso\":\"" + fact.idCompromiso() + "\",\"folio\":\"" + fact.folio()
                + "\",\"fechaVencimiento\":\"" + fact.fechaVencimiento() + "\",\"estado\":\"" + fact.estado()
                + "\",\"pago\":" + payment + "}";
    }
}
