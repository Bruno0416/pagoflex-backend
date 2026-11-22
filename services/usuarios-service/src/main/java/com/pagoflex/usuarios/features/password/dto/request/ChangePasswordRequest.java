package com.pagoflex.usuarios.features.password.dto.request;

import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria")
        String contrasenaActual,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @StrongPassword
        String nuevaContrasena,

        @NotBlank(message = "La confirmacion de contrasena es obligatoria")
        String confirmacionContrasena
) {}
