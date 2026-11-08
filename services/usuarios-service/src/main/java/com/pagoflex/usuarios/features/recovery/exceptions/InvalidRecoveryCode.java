package com.pagoflex.usuarios.features.recovery.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class InvalidRecoveryCode extends PagoFlexException {

    public InvalidRecoveryCode(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
