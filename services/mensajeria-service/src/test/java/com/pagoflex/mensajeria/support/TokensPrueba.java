package com.pagoflex.mensajeria.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/// Firma tokens con los mismos claims que emite usuarios-service
public final class TokensPrueba {
    public static final String SECRETO = "test-only-jwt-secret-that-has-at-least-32-bytes";
    public static final String CLAVE_INTERNA = "test-only-internal-api-key";

    private TokensPrueba() {
    }

    public static String acceso(UUID usuarioId, String correo, String rol) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuarioId.toString())
                .claim("token_type", "ACCESS")
                .claim("userId", usuarioId.toString())
                .claim("email", correo)
                .claim("roles", List.of(rol))
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(Duration.ofMinutes(15))))
                .signWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }

    public static String bearer(UUID usuarioId, String correo, String rol) {
        return "Bearer " + acceso(usuarioId, correo, rol);
    }
}
