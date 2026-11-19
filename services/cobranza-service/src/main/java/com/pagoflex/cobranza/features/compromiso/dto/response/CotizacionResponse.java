package com.pagoflex.cobranza.features.compromiso.dto.response;

import java.time.LocalDate;

/// total calculado por el servidor al momento de consultar RN-03
public record CotizacionResponse(
        String folio,
        String empresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        long monto,
        long recargo,
        long total,
        LocalDate fechaVencimiento,
        LocalDate fechaConsulta,
        String cotizacionId
) {
}
