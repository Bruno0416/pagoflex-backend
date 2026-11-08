package com.pagoflex.usuarios.features.password.dto.request;

import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank
        String changeToken,

        @NotBlank
        @StrongPassword
        String newPassword
) {
}
