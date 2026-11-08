package com.pagoflex.usuarios.features.password.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class PasswordReuseNotAllowed extends PagoFlexException {

    public PasswordReuseNotAllowed() {
        super(HttpStatus.BAD_REQUEST.value(), "La nueva contraseña debe ser diferente");
    }
}
