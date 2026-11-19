package com.pagoflex.usuarios.features.recovery.service.code;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

// Evita enviar codigos continuamente al mismo correo sin revelar si la cuenta existe.
@Service
@RequiredArgsConstructor
public class RecoverySendThrottle {
    private static final String PREFIX = "pagoflex:auth:send-cooldown:";
    private static final Duration COOLDOWN = Duration.ofSeconds(60);
    private final StringRedisTemplate redis;

    public boolean allow(String normalizedEmail) {
        // SET NX con expiracion es atomico incluso con varias replicas del servicio.
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(
                PREFIX + sha256(normalizedEmail), "1", COOLDOWN));
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
