package com.pagoflex.usuarios.features.password.dto.request;

import jakarta.validation.constraints.NotBlank;


public record VerifyCurrentPasswordRequest(@NotBlank(message = "La contraseña no puede estar vacía") String password) {
}