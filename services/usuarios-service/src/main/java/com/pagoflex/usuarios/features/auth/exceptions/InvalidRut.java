package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.FieldErrorException;
import org.springframework.http.HttpStatus;

public class InvalidRut extends FieldErrorException {
    public InvalidRut() {
        super(HttpStatus.BAD_REQUEST.value(), "rut", "El rut tiene un formato no valido");
    }

    public InvalidRut(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "rut", message);
    }
}
