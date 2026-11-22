package com.pagoflex.contracts.organizacion;

import java.util.UUID;

public record RespuestaValidacionCodigoEmpresa(
        UUID idEmpresa, String nombreEmpresa,
        TipoOrganizacion tipoEmpresa, RolAutorizado rolAutorizado) {}
