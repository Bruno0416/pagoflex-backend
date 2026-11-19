package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidRut extends PagoFlexException {
    public InvalidRut() {
        super(HttpStatus.BAD_REQUEST.value(), "El rut no es válido");
    }
}
