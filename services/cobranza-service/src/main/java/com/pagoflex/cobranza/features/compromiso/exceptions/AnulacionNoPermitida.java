package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class AnulacionNoPermitida extends PagoFlexException {
    public AnulacionNoPermitida(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
}
