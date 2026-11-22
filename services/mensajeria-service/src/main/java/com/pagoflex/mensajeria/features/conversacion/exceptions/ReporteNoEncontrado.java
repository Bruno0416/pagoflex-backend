package com.pagoflex.mensajeria.features.conversacion.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class ReporteNoEncontrado extends PagoFlexException {
    public ReporteNoEncontrado() {
        super(404, "No se encontró el reporte");
    }
}
