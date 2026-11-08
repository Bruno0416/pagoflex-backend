package com.pagoflex.usuarios.features.password.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidChangeToken extends PagoFlexException {

    public InvalidChangeToken() {
        super(HttpStatus.BAD_REQUEST.value(), "Token inválido o expirado");
    }
}
