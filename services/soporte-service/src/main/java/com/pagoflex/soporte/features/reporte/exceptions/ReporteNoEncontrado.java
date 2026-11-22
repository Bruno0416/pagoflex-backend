package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ReporteNoEncontrado extends PagoFlexException {
    public ReporteNoEncontrado(String numero) {
        super(HttpStatus.NOT_FOUND.value(), "No se encontró el reporte " + numero);
    }
}
