package com.pagoflex.core.error;

public class FieldErrorException extends PagoFlexException {
    private final String field;

    public FieldErrorException(int status, String field, String message) {
        super(status, message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
