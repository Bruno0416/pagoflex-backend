package com.pagoflex.usuarios.features.password.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria")
        @Schema(description = "Contraseña vigente de la cuenta autenticada", example = "Demo123!")
        String contrasenaActual,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @StrongPassword
        @Schema(description = "Nueva contraseña distinta de la actual y que cumple los requisitos de seguridad", example = "Ejemplo123!")
        String nuevaContrasena
) {}
