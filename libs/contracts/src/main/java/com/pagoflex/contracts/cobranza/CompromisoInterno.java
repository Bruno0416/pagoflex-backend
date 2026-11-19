package com.pagoflex.contracts.cobranza;

import java.time.LocalDate;

/// Datos de un compromiso que cobranza-service comparte con otros servicios.
/// El estado y el total ya consideran el vencimiento y el recargo a la fecha.
public record CompromisoInterno(
        String folio,
        String empresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        long total,
        LocalDate fechaVencimiento,
        String estado,
        String nombrePersona,
        String correoPersona,
        String telefonoPersona
) {
}
