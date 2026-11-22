package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.PagoFlexException;

public class OrganizationServiceUnavailable extends PagoFlexException {
    public OrganizationServiceUnavailable() {
        super(503, "No fue posible validar el codigo de empresa");
    }
}
