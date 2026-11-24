package com.pagoflex.usuarios.features.recovery.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @Schema(description = "Token temporal de un solo uso recibido al verificar el código. Ejemplo ficticio", example = "lanwr1CsLhjHi9NvuNYll96VfilAT1jHCMAYD6VUsaI")
        @NotBlank(message = "El token de restablecimiento es obligatorio")
        String tokenRestablecimiento,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        @Schema(description = "Nueva contraseña que cumple los requisitos de seguridad", example = "Ejemplo123!")
        String nuevaContrasena
) {}
