package com.pagoflex.contracts.organization;

import java.util.UUID;

public record ValidateOrganizationCodeResponse(
        UUID idEmpresa, String nombreEmpresa,
        OrganizationType tipoEmpresa, AuthorizedRole rolAutorizado) {}
