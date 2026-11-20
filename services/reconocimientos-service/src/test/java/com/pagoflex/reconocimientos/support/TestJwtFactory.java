package com.pagoflex.reconocimientos.support;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;

public final class TestJwtFactory {
    public static final String SECRET = "test-only-jwt-secret-that-has-at-least-32-bytes";
    private static final NimbusJwtEncoder ENCODER = new NimbusJwtEncoder(new ImmutableSecret<>(
            new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));

    private TestJwtFactory() {
    }

    // ---------- tokens validos ----------
    public static String access(ValueOperations<String, String> redis, UUID userId, String role) {
        return build(redis, userId, "ACCESS", List.of(role), Instant.now().plusSeconds(600), true);
    }

    // ---------- variantes invalidas ----------
    public static String expired(ValueOperations<String, String> redis, UUID userId, String role) {
        return build(redis, userId, "ACCESS", List.of(role), Instant.now().minusSeconds(900), true);
    }

    public static String refreshType(ValueOperations<String, String> redis, UUID userId, String role) {
        return build(redis, userId, "REFRESH", List.of(role), Instant.now().plusSeconds(600), true);
    }

    public static String withoutSession(ValueOperations<String, String> redis, UUID userId, String role) {
        return build(redis, userId, "ACCESS", List.of(role), Instant.now().plusSeconds(600), false);
    }

    // --- helper ---
    private static String build(ValueOperations<String, String> redis, UUID userId, String type,
                                List<String> roles, Instant expiresAt, boolean registerSession) {
        // 1. generar los identificadores de token y sesion
        UUID sid = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        // 2. registrar la sesion como lo hace usuarios-service
        if (registerSession) {
            when(redis.get("auth:access:" + jti)).thenReturn(sid.toString());
            when(redis.get("auth:session:" + sid)).thenReturn(userId + "|" + jti + "|hash");
        }
        // 3. firmar el token con los claims esperados
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(userId.toString())
                .id(jti.toString())
                .claim("sid", sid.toString())
                .claim("token_type", type)
                .claim("userId", userId.toString())
                .claim("email", "prueba@example.com")
                .claim("roles", roles)
                .issuedAt(Instant.now().minusSeconds(1000))
                .expiresAt(expiresAt)
                .build();
        return ENCODER.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
