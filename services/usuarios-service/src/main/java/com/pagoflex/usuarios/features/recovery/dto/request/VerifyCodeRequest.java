package com.pagoflex.usuarios.features.recovery.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record VerifyCodeRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        String email,
        @NotBlank(message = "El código es obligatorio") String code) {
}
