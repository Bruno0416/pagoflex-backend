package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class DatosCompromisoInvalidos extends PagoFlexException {
    public DatosCompromisoInvalidos(String message) {
        super(HttpStatus.BAD_REQUEST.value(), message);
    }
}
