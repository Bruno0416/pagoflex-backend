package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class EmailAlreadyInUse extends PagoFlexException {

    public EmailAlreadyInUse(String email) {
        super(HttpStatus.CONFLICT.value(), "El email " + email + " ya está registrado.");
    }
}
