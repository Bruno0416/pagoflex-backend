package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.usuarios.core.cache.ExpiringCache;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;

@Service
public class ChangeTokenService {
    private static final Duration TIME = Duration.ofMinutes(5);
    private static final String PREFIX = "pagoflex:auth:password-change:";
    private final SecureRandom secureRandom = new SecureRandom();
    private final ExpiringCache cache;

    public ChangeTokenService(ExpiringCache cache) {
        this.cache = Objects.requireNonNull(cache);
    }

    // ---------- creación y consumo de tokens ----------
    public String issueToken(UUID userId, String currentPasswordHash) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(currentPasswordHash, "currentPasswordHash");

        // 1. generar una credencial opaca vinculada al usuario
        String token = generateToken();

        // 2. guardar solo el resumen de la credencial y del hash actual
        cache.put(key(userId, token), digest(currentPasswordHash), TIME);
        return token;
    }

    public boolean consumeToken(UUID userId, String token, String currentPasswordHash) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(currentPasswordHash, "currentPasswordHash");
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) {
            return false;
        }
        // 1. consumir la credencial una sola vez si la contraseña no cambio
        return cache.consumeIfMatches(key(userId, token), digest(currentPasswordHash));
    }

    public long expiresInSeconds() {
        return TIME.toSeconds();
    }

    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // --- helpers ---
    private static String key(UUID userId, String token) {
        return PREFIX + userId + ":" + digest(token);
    }

    private static String digest(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
