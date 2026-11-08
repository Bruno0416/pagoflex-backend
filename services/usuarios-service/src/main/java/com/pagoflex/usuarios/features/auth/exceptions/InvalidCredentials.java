package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidCredentials extends PagoFlexException {

    public InvalidCredentials() {
        super(HttpStatus.UNAUTHORIZED.value(), "Credenciales inválidas");
    }

    public InvalidCredentials(String message) {
        super(HttpStatus.UNAUTHORIZED.value(), message);
    }
}