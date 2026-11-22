package com.pagoflex.soporte.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;
import org.springframework.http.HttpStatus;

public class CobroNoReportable extends PagoFlexException {
    public CobroNoReportable(String message) {
        super(HttpStatus.CONFLICT.value(), message);
    }
}
