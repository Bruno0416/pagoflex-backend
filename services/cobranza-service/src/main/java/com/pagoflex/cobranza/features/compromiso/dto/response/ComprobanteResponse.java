package com.pagoflex.cobranza.features.compromiso.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

/// datos fijos del comprobante, tal como quedaron al pagar RN-06
public record ComprobanteResponse(
        String numero,
        String folio,
        String empresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        String personaNombre,
        String personaRut,
        LocalDate fechaVencimiento,
        long monto,
        long recargo,
        long total,
        String canal,
        String medio,
        LocalDateTime fechaPago
) {
}
