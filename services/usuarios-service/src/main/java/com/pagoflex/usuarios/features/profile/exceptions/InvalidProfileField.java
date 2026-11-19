package com.pagoflex.usuarios.features.profile.exceptions;

import com.pagoflex.core.error.FieldErrorException;
import org.springframework.http.HttpStatus;

public class InvalidProfileField extends FieldErrorException {
    public InvalidProfileField(String field, String message) {
        super(HttpStatus.BAD_REQUEST.value(), field, message);
    }
}
