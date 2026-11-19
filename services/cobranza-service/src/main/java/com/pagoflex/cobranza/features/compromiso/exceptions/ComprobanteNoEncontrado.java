package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ComprobanteNoEncontrado extends PagoFlexException {
    public ComprobanteNoEncontrado(String numero) {
        super(HttpStatus.NOT_FOUND.value(), "No se encontró el comprobante " + numero);
    }
}
