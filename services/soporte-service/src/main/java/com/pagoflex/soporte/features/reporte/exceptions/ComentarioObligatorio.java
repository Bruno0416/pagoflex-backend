package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ComentarioObligatorio extends PagoFlexException {
    public ComentarioObligatorio() {
        super(HttpStatus.BAD_REQUEST.value(), "Con el motivo Otro tienes que contar qué pasó");
    }
}
