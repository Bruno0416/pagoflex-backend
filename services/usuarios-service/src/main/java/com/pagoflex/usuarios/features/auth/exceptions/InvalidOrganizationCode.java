package com.pagoflex.usuarios.features.auth.exceptions;

import com.pagoflex.core.error.FieldErrorException;

public class InvalidOrganizationCode extends FieldErrorException {
    public InvalidOrganizationCode() {
        super(400, "codigoEmpresa", "El codigo de empresa no es valido");
    }
}
