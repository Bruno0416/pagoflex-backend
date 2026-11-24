package com.pagoflex.usuarios.features.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo no puede estar vacío")
        @Email(message = "Debe ser un correo válido: email@example.com")
        @Schema(description = "Correo de la cuenta", example = "camila.ejemplo@example.com")
        String correo,

        @Schema(description = "Contraseña vigente de la cuenta", example = "Demo123!")
        @NotBlank(message = "La contraseña no puede estar vacía")
        String contrasena
) {}
