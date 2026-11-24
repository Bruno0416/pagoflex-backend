package com.pagoflex.usuarios.features.recovery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyCodeRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        @Schema(description = "Correo utilizado al solicitar la recuperación", example = "camila.ejemplo@example.com")
        String correo,
        @Schema(description = "Código de recuperación recibido por correo. Ejemplo ficticio", example = "ABC123")
        @NotBlank(message = "El código es obligatorio")
        String codigo
) {}
