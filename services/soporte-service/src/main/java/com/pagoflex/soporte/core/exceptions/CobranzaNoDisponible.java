package com.pagoflex.soporte.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CobranzaNoDisponible extends PagoFlexException {
    public CobranzaNoDisponible() {
        super(HttpStatus.SERVICE_UNAVAILABLE.value(), "No pudimos consultar el cobro. Intenta de nuevo en unos minutos.");
    }
}
