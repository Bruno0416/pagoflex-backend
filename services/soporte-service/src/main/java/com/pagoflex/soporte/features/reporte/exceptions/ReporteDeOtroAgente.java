package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ReporteDeOtroAgente extends PagoFlexException {
    public ReporteDeOtroAgente() {
        super(HttpStatus.FORBIDDEN.value(), "Este reporte lo está gestionando otro agente");
    }
}
