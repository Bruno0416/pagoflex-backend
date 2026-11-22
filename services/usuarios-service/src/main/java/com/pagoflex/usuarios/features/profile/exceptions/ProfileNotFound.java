package com.pagoflex.usuarios.features.profile.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ProfileNotFound extends PagoFlexException {
    public ProfileNotFound() {
        super(HttpStatus.NOT_FOUND.value(), "Perfil no encontrado");
    }
}
