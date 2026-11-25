package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ComprobanteInvalido extends PagoFlexException {
    public ComprobanteInvalido(String mensaje) {
        super(HttpStatus.BAD_REQUEST.value(), mensaje);
    }
}
