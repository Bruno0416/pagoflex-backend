package com.pagoflex.usuarios.features.internal.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class PersonNotFound extends PagoFlexException {
    public PersonNotFound() { super(404, "Persona no encontrada"); }
}
