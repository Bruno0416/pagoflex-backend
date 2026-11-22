package com.pagoflex.reconocimientos.features.recognition.domain;

import com.pagoflex.contracts.cobranza.HechoCompromiso;
import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.contracts.cobranza.RespuestaHechosPago;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaInvalidData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class PaymentFactsValidator {
    private PaymentFactsValidator() {
    }

    // ---------- validacion del contrato ----------
    public static void validate(RespuestaHechosPago facts, UUID expectedUser) {
        // 1. la respuesta debe corresponder al usuario solicitado
        if (facts == null || facts.idUsuario() == null || !facts.idUsuario().equals(expectedUser)
                || facts.compromisos() == null) {
            throw new CobranzaInvalidData();
        }
        // 2. cada compromiso debe estar completo, sin duplicados y con pago coherente con su estado
        Set<UUID> seen = new HashSet<>();
        for (HechoCompromiso fact : facts.compromisos()) {
            if (fact == null || fact.idCompromiso() == null || fact.folio() == null || fact.folio().isBlank()
                    || fact.fechaVencimiento() == null || fact.estado() == null
                    || !seen.add(fact.idCompromiso())) {
                throw new CobranzaInvalidData();
            }
            boolean paid = fact.estado() == EstadoCompromisoEvaluacion.PAGADO;
            if (paid != (fact.pago() != null)) throw new CobranzaInvalidData();
            if (paid && (fact.pago().fechaHoraPago() == null || fact.pago().numeroComprobante() == null
                    || fact.pago().numeroComprobante().isBlank())) {
                throw new CobranzaInvalidData();
            }
        }
    }
}
