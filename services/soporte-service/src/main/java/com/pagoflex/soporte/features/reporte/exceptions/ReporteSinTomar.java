package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ReporteSinTomar extends PagoFlexException {
    public ReporteSinTomar() {
        super(HttpStatus.CONFLICT.value(), "Primero tienes que tomar el reporte");
    }
}
