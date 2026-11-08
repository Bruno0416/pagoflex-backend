package com.pagoflex.usuarios.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class UserNotFound extends PagoFlexException {

    public UserNotFound(String email) {
        super(HttpStatus.NOT_FOUND.value(), "No se encontró el usuario con correo " + email);
    }

    public UserNotFound() {
        super(HttpStatus.NOT_FOUND.value(), "Usuario no encontrado");
    }
}
