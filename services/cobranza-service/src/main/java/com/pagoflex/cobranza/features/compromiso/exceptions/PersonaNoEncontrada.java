package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class PersonaNoEncontrada extends PagoFlexException {
    public PersonaNoEncontrada() {
        super(HttpStatus.NOT_FOUND.value(), "No hay una persona registrada con ese RUT");
    }
}
