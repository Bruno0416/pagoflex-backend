package com.pagoflex.usuarios.features.profile.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class AvatarNotFound extends PagoFlexException {
    public AvatarNotFound() {
        super(HttpStatus.NOT_FOUND.value(), "Avatar no encontrado");
    }
}
