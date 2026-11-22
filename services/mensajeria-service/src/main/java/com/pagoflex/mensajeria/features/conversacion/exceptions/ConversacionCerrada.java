package com.pagoflex.mensajeria.features.conversacion.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class ConversacionCerrada extends PagoFlexException {
    public ConversacionCerrada() {
        super(409, "El reporte está resuelto: la conversación es solo de lectura");
    }
}
