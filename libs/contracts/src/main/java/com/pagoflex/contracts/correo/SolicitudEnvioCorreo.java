package com.pagoflex.contracts.correo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SolicitudEnvioCorreo(
        @Email @NotBlank String to,
        @NotNull TipoCorreo type,
        @NotNull Map<String, @NotBlank String> variables
) {
}
