package com.pagoflex.mensajeria.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class ServicioNoDisponible extends PagoFlexException {
    public ServicioNoDisponible() {
        super(503, "No pudimos validar el reporte en este momento, intenta de nuevo");
    }
}
