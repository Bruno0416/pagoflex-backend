package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class InvalidSession extends PagoFlexException {
    public InvalidSession() { super(401, "Sesion invalida o expirada"); }
}
