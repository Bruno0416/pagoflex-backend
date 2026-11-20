package com.pagoflex.soporte.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class EjecutivoSinEmpresa extends PagoFlexException {
    public EjecutivoSinEmpresa() {
        super(HttpStatus.FORBIDDEN.value(), "Tu cuenta no tiene una empresa asignada");
    }
}
