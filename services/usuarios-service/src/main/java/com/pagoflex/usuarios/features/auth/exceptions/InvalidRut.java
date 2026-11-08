package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidRut extends PagoFlexException {
    public InvalidRut() {
        super(HttpStatus.BAD_REQUEST.value(), "El rut tiene un formato no valido");
    }

    public InvalidRut(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
