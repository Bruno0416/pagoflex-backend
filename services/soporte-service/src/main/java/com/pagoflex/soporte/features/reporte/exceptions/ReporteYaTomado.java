package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ReporteYaTomado extends PagoFlexException {
    public ReporteYaTomado() {
        super(HttpStatus.CONFLICT.value(), "Este reporte ya fue tomado por un agente");
    }
}
