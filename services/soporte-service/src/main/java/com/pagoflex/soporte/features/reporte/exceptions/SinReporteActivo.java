package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class SinReporteActivo extends PagoFlexException {
    public SinReporteActivo(String folio) {
        super(HttpStatus.NOT_FOUND.value(), "El compromiso " + folio + " no tiene un reporte activo");
    }
}
