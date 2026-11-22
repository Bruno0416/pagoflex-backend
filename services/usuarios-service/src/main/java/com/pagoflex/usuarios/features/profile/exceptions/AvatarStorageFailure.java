package com.pagoflex.usuarios.features.profile.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class AvatarStorageFailure extends PagoFlexException {
    public AvatarStorageFailure() {
        super(HttpStatus.INTERNAL_SERVER_ERROR.value(), "No se pudo procesar el avatar");
    }
}
