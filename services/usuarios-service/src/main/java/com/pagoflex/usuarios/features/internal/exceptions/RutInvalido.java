package com.pagoflex.usuarios.features.internal.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class RutInvalido extends PagoFlexException {
    public RutInvalido() {
        super(400, "El RUT no es válido");
    }
}
