package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ReporteYaExiste extends PagoFlexException {
    public ReporteYaExiste(String numero) {
        super(HttpStatus.CONFLICT.value(), "Ya tienes un reporte abierto de este cobro: " + numero);
    }
}
