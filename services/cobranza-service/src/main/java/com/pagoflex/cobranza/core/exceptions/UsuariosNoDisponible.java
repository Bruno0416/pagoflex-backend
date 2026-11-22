package com.pagoflex.cobranza.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class UsuariosNoDisponible extends PagoFlexException {
    public UsuariosNoDisponible() {
        super(HttpStatus.SERVICE_UNAVAILABLE.value(), "No pudimos consultar los datos de la persona, intenta de nuevo");
    }
}
