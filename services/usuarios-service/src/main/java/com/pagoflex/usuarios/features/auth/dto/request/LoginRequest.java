package com.pagoflex.usuarios.features.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "El correo no puede estar vacío")
        @Email(message = "Debe ser un correo válido: email@example.com")
        String email,

        @NotBlank(message = "La contraseña no puede estar vacía") String password) {
}
