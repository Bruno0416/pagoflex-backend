package com.pagoflex.usuarios.features.password.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class AuthenticatedUserNotFound extends PagoFlexException {

    public AuthenticatedUserNotFound() {
        super(HttpStatus.UNAUTHORIZED.value(), "Usuario no encontrado");
    }
}
