package com.pagoflex.cobranza.core.exceptions;

import com.pagoflex.core.error.PagoFlexException;

import java.util.Map;

/// Error de negocio con un código estable para que la app sepa qué mostrar
public class ReglaRechazada extends PagoFlexException {
    private final String code;
    private final Map<String, Object> details;

    public ReglaRechazada(int status, String code, String message) {
        this(status, code, message, Map.of());
    }

    public ReglaRechazada(int status, String code, String message, Map<String, Object> details) {
        super(status, message);
        this.code = code;
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
