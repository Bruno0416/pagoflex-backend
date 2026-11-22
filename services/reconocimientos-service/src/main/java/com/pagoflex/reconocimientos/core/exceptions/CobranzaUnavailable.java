package com.pagoflex.reconocimientos.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class CobranzaUnavailable extends PagoFlexException {
    public CobranzaUnavailable() {
        super(503, "Cobranza no está disponible; reintenta más tarde");
    }
}
