package com.pagoflex.usuarios.features.recovery.dto.request;

import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "El token de restablecimiento es obligatorio") String tokenRestablecimiento,
        @NotBlank(message = "La nueva contraseña es obligatoria")
        @StrongPassword
        String nuevaContrasena) {
}
