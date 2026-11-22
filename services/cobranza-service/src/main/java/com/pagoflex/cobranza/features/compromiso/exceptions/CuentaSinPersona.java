package com.pagoflex.cobranza.features.compromiso.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CuentaSinPersona extends PagoFlexException {
    public CuentaSinPersona() {
        super(HttpStatus.NOT_FOUND.value(), "Tu cuenta todavía no tiene datos personales asociados");
    }
}
