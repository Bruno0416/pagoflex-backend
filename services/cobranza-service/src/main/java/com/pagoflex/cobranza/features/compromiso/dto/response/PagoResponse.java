package com.pagoflex.cobranza.features.compromiso.dto.response;

import com.pagoflex.cobranza.features.compromiso.entity.CanalPago;
import com.pagoflex.cobranza.features.compromiso.entity.MedioPago;

import java.time.LocalDateTime;

public record PagoResponse(
        String numeroComprobante,
        String folio,
        String empresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        long montoPagado,
        LocalDateTime fechaHora,
        CanalPago canal,
        MedioPago medio,
        String estado
) {
}
