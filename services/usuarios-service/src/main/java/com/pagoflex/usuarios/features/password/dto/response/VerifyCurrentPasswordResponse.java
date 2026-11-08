package com.pagoflex.usuarios.features.password.dto.response;

public record VerifyCurrentPasswordResponse(
        String changeToken,
        Long expiresIn
) {
}
