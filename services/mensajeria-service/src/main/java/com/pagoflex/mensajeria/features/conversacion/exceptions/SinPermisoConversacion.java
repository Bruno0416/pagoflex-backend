package com.pagoflex.mensajeria.features.conversacion.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class SinPermisoConversacion extends PagoFlexException {
    public SinPermisoConversacion() {
        super(403, "No participas en la conversación de este reporte");
    }
}
