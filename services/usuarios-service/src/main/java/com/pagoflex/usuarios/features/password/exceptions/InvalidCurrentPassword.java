package com.pagoflex.usuarios.features.password.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidCurrentPassword extends PagoFlexException {

    public InvalidCurrentPassword() {
        super(HttpStatus.BAD_REQUEST.value(), "Contraseña actual incorrecta");
    }
}
