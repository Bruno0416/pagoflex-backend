package com.pagoflex.cobranza.support;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;

/// Firma tokens con los mismos claims que emite usuarios-service
public final class TokensPrueba {
    public static final String SECRETO = "test-only-jwt-secret-that-has-at-least-32-bytes";
    public static final String CLAVE_INTERNA = "test-only-internal-api-key";

    /// sesiones que usuarios-service dejaría en redis; las lee RedisPrueba
    public static final Map<String, String> SESIONES = new ConcurrentHashMap<>();

    private TokensPrueba() {
    }

    public static String acceso(UUID usuarioId, String correo, String rol) {
        Instant ahora = Instant.now();
        // 1. el token queda ligado a una sesión vigente, como lo emite usuarios-service
        UUID jti = UUID.randomUUID();
        UUID sid = UUID.randomUUID();
        SESIONES.put("auth:access:" + jti, sid.toString());
        SESIONES.put("auth:session:" + sid, usuarioId + "|" + jti + "|hash");
        return Jwts.builder()
                .subject(usuarioId.toString())
                .id(jti.toString())
                .claim("sid", sid.toString())
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
