package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CompromisoDuplicado extends PagoFlexException {
    public CompromisoDuplicado() {
        super(HttpStatus.CONFLICT.value(), "Ese compromiso ya está registrado para esta persona");
    }
}
