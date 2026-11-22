package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.FieldErrorException;
import org.springframework.http.HttpStatus;

public class RutAlreadyInUse extends FieldErrorException {
    public RutAlreadyInUse() {
        super(HttpStatus.CONFLICT.value(), "rut", "El RUT ya está registrado");
    }
}
