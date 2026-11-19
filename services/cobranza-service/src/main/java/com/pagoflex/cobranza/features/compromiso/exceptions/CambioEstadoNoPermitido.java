package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CambioEstadoNoPermitido extends PagoFlexException {
    public CambioEstadoNoPermitido(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
}
