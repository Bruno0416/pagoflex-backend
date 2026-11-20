package com.pagoflex.cobranza.features.compromiso.dto.response;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;

import java.time.LocalDate;
import java.time.LocalDateTime;

/// Compromiso como lo ve su dueño, con lo necesario para saber si puede pagarlo
public record MiCompromisoResponse(
        String folio,
        String empresaCodigo,
        String empresa,
        String correoAtencion,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        long monto,
        long recargo,
        long total,
        LocalDate fechaVencimiento,
        EstadoCompromiso estado,
        long diasAtraso,
        boolean pagable,
        String folioAnterior,
        String numeroComprobante,
        LocalDateTime fechaPago
) {
}
