package com.pagoflex.cobranza.features.empresa.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class EjecutivoSinEmpresa extends PagoFlexException {
    public EjecutivoSinEmpresa() {
        super(HttpStatus.FORBIDDEN.value(), "Tu usuario no tiene una empresa asociada");
    }
}
