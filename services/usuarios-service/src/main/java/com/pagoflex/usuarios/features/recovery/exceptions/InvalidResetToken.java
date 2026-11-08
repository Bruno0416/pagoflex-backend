package com.pagoflex.usuarios.features.recovery.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidResetToken extends PagoFlexException {

    public InvalidResetToken() {
        super(HttpStatus.BAD_REQUEST.value(), "El enlace temporal de recuperación es inválido o ha expirado");
    }
}
