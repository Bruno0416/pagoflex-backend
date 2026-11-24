package com.pagoflex.usuarios.features.recovery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RecoveryRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        @Schema(description = "Correo de la cuenta que se desea recuperar", example = "camila.ejemplo@example.com")
        String correo
) {}
