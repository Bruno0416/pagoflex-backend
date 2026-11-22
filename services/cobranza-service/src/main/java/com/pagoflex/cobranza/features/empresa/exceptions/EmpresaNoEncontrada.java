package com.pagoflex.cobranza.features.empresa.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class EmpresaNoEncontrada extends PagoFlexException {
    public EmpresaNoEncontrada(String codigo) {
        super(HttpStatus.NOT_FOUND.value(), "No existe la empresa " + codigo);
    }
}
