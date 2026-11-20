package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class RespuestaObligatoria extends PagoFlexException {
    public RespuestaObligatoria() {
        super(HttpStatus.BAD_REQUEST.value(), "Escribe la respuesta para el usuario antes de resolver");
    }
}
