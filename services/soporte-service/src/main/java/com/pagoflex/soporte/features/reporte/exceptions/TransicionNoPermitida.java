package com.pagoflex.soporte.features.reporte.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import org.springframework.http.HttpStatus;

public class TransicionNoPermitida extends PagoFlexException {
    public TransicionNoPermitida(EstadoReporte desde, EstadoReporte hacia) {
        super(HttpStatus.CONFLICT.value(), "El reporte está " + desde + " y no puede pasar a " + hacia);
    }
}
