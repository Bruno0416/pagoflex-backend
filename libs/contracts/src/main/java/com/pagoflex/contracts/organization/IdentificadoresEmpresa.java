package com.pagoflex.contracts.organization;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/// Identificadores de organización que conocen varios servicios.
public final class IdentificadoresEmpresa {
    /// Organización interna de PagoFlex: pertenece a todos los agentes.
    public static final UUID INTERNA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    public static final String NOMBRE_INTERNA = "PagoFlex";

    private IdentificadoresEmpresa() {
    }

    /// Solo para los datos de ejemplo: mismo id de empresa en cobranza y usuarios sin llamadas entre ellos.
    public static UUID deCodigoDemo(String codigoEmpresa) {
        return UUID.nameUUIDFromBytes(("pagoflex:empresa-demo:" + codigoEmpresa).getBytes(StandardCharsets.UTF_8));
    }
}
