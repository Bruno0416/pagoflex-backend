package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.FieldErrorException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUse extends FieldErrorException {
    public EmailAlreadyInUse(String email) {
        super(HttpStatus.CONFLICT.value(), "correo", "El correo " + email + " ya está registrado");
    }
}
