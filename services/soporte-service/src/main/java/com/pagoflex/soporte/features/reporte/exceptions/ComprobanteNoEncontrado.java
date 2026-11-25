package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class ComprobanteNoEncontrado extends PagoFlexException {
    public ComprobanteNoEncontrado() {
        super(HttpStatus.NOT_FOUND.value(), "El reporte no tiene comprobante");
    }
}
