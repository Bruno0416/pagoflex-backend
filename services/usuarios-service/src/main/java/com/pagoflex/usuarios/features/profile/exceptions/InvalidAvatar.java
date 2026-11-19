package com.pagoflex.usuarios.features.profile.exceptions;

import com.pagoflex.core.error.FieldErrorException;
import org.springframework.http.HttpStatus;

public class InvalidAvatar extends FieldErrorException {
    public InvalidAvatar(String message) {
        super(HttpStatus.BAD_REQUEST.value(), "avatar", message);
    }
}
