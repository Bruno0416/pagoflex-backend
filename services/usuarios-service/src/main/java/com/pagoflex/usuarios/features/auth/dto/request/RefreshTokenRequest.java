package com.pagoflex.usuarios.features.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshTokenRequest(
        @Schema(description = "Token de renovación opaco obtenido al iniciar o renovar sesión. Ejemplo ficticio; usar el token vigente", example = "wBCHxwd4wEn0M1xkUYspG830Jcrqw1Vn6eXGVDTo5Wo")
        @NotBlank @Size(max = 128)
        String tokenRenovacion
) {}
