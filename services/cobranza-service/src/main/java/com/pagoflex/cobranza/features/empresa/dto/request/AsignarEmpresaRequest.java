package com.pagoflex.cobranza.features.empresa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/// mientras no esté el registro con código de empresa, se asigna por ruta interna
public record AsignarEmpresaRequest(
        @NotNull UUID usuarioId,
        @NotBlank String codigoEmpresa
) {
}
