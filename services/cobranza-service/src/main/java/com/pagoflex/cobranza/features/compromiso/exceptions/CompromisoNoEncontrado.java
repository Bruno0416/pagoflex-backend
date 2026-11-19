package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CompromisoNoEncontrado extends PagoFlexException {
    public CompromisoNoEncontrado(String folio) {
        super(HttpStatus.NOT_FOUND.value(), "No se encontró el compromiso " + folio);
    }
}
