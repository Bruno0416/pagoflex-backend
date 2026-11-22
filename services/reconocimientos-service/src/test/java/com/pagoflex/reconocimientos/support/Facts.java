package com.pagoflex.reconocimientos.support;

import com.pagoflex.contracts.cobranza.HechoCompromiso;
import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.contracts.cobranza.HechoPago;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public final class Facts {
    private Facts() {
    }

    public static HechoCompromiso paid(String folio, String due, String paidAt) {
        return new HechoCompromiso(UUID.randomUUID(), folio, LocalDate.parse(due), EstadoCompromisoEvaluacion.PAGADO,
                new HechoPago("CMP-" + folio, OffsetDateTime.parse(paidAt)));
    }

    public static HechoCompromiso withStatus(String folio, String due, EstadoCompromisoEvaluacion status) {
        return new HechoCompromiso(UUID.randomUUID(), folio, LocalDate.parse(due), status, null);
    }

    // ---------- cuerpo json del contrato ----------
    public static String body(UUID userId, HechoCompromiso... facts) {
        // 1. serializar cada hecho con los nombres del contrato
        String items = Arrays.stream(facts).map(Facts::json).collect(Collectors.joining(","));
        return "{\"idUsuario\":\"" + userId + "\",\"generadoEn\":\"2026-10-06T09:00:00-03:00\",\"compromisos\":["
                + items + "]}";
    }

    // --- helper ---
    private static String json(HechoCompromiso fact) {
        String payment = fact.pago() == null ? "null" : "{\"numeroComprobante\":\"" + fact.pago().numeroComprobante()
                + "\",\"fechaHoraPago\":\"" + fact.pago().fechaHoraPago() + "\"}";
        return "{\"idCompromiso\":\"" + fact.idCompromiso() + "\",\"folio\":\"" + fact.folio()
                + "\",\"fechaVencimiento\":\"" + fact.fechaVencimiento() + "\",\"estado\":\"" + fact.estado()
                + "\",\"pago\":" + payment + "}";
    }
}
