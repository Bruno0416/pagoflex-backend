package com.pagoflex.reconocimientos.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class CobranzaInvalidData extends PagoFlexException {
    public CobranzaInvalidData() {
        super(502, "Cobranza devolvió datos inconsistentes");
    }
}
