package com.pagoflex.usuarios.features.internal.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class PersonaNoEncontrada extends PagoFlexException {
    public PersonaNoEncontrada() {
        super(404, "No existe una persona con esos datos");
    }
}
