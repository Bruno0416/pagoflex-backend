package com.pagoflex.core.error;

public class PagoFlexException extends RuntimeException {
    private final int status;

    public PagoFlexException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
