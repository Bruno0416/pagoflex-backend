package com.pagoflex.usuarios.features.internal.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class PersonaYaExiste extends PagoFlexException {
    public PersonaYaExiste() {
        super(409, "Ya existe una persona con ese RUT");
    }
}
