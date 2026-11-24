package com.pagoflex.usuarios.features.recovery.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record VerifyResetTokenResponse(
        @Schema(description = "Token temporal para restablecer la contraseña. Ejemplo ficticio, no válido", example = "lanwr1CsLhjHi9NvuNYll96VfilAT1jHCMAYD6VUsaI")
        String tokenRestablecimiento
) {}
