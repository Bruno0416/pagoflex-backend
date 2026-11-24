package com.pagoflex.usuarios.features.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pagoflex.usuarios.core.security.Role;

public record AuthResponse(
        @Schema(description = "JWT de acceso para Authorization: Bearer. Ejemplo ficticio, no válido", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJlamVtcGxvIn0.firma-de-ejemplo")
        String tokenAcceso,
        @Schema(description = "Token opaco para renovar la sesión. Ejemplo ficticio, no válido", example = "wBCHxwd4wEn0M1xkUYspG830Jcrqw1Vn6eXGVDTo5Wo")
        String tokenRenovacion,
        @Schema(description = "Rol asignado a la cuenta", example = "USUARIO_FINAL")
        Role rol
) {}
