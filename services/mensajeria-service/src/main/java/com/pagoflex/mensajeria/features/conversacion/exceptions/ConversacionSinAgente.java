package com.pagoflex.mensajeria.features.conversacion.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class ConversacionSinAgente extends PagoFlexException {
    public ConversacionSinAgente() {
        super(409, "El reporte todavía no tiene un agente asignado");
    }
}
