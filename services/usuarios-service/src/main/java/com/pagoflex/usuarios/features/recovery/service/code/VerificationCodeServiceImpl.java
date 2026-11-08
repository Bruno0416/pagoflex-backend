package com.pagoflex.usuarios.features.recovery.service.code;

import com.pagoflex.contracts.email.CodeType;
import com.pagoflex.usuarios.core.cache.ExpiringCache;
import com.pagoflex.usuarios.core.cache.RedisTransactionExecutor;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidRecoveryCode;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import com.pagoflex.usuarios.features.recovery.utils.CodeGeneratorUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VerificationCodeServiceImpl implements VerificationCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_PREFIX = "pagoflex:auth:code:";
    private static final String ACTIVE_PREFIX = "pagoflex:auth:active:";
    private static final String TOKEN_PREFIX = "pagoflex:auth:reset:";

    private final RedisTransactionExecutor transactions;
    private final ExpiringCache cache;

    @Value("${verification.code.expiration-minutes}")
    private long codeExpirationMinutes;

    @Value("${verification.reset.expiration-minutes}")
    private long resetExpirationMinutes;

    // ---------- generación de códigos ----------
    @Override
    public String generateCode(String email, CodeType type) {
        // 1. generar código y preparar claves con su expiración
        Duration expiration = Duration.ofSeconds(seconds(codeExpirationMinutes));
        String code = CodeGeneratorUtil.generateCode();
        String reference = reference(email, type);
        String codeKey = CODE_PREFIX + reference;
        String activeKey = ACTIVE_PREFIX + reference;

        // 2. reemplazar el código e invalidar el token activo en una transacción redis
        Boolean stored = transactions.execute(operations -> {
            List<Object> results = transactions.commit(operations, () -> {
                operations.delete(activeKey);
                operations.opsForValue().set(codeKey, digest(code), expiration);
            });
            return results != null && results.size() == 2;
        });
        // 3. rechazar el registro si la transacción no se confirma
        if (!Boolean.TRUE.equals(stored)) {
            throw new IllegalStateException("No se pudo guardar el código de recuperación");
        }
        return code;
    }

    // ---------- verificación de códigos ----------
    @Override
    public String verifyCodeAndIssueResetToken(String email, CodeType type, String submittedCode) {
        // 1. normalizar el código y preparar las claves de verificación
        Duration expiration = Duration.ofSeconds(seconds(resetExpirationMinutes));
        String normalized = submittedCode == null ? "" : submittedCode.trim().toUpperCase(Locale.ROOT);
        String reference = reference(email, type);
        String codeKey = CODE_PREFIX + reference;
        String activeKey = ACTIVE_PREFIX + reference;

        // 2. generar un token temporal para recuperar la contraseña
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String tokenHash = digest(resetToken);
        String tokenKey = TOKEN_PREFIX + tokenHash;

        // 3. verificar y consumir el código de forma atomica antes de emitir el token
        Boolean verified = transactions.execute(operations -> {
            operations.watch(List.of(codeKey, activeKey));
            try {
                if (!Objects.equals(operations.opsForValue().get(codeKey), digest(normalized))) {
                    return false;
                }
                List<Object> results = transactions.commit(operations, () -> {
                    operations.delete(codeKey);
                    operations.opsForValue().set(tokenKey, email, expiration);
                    operations.opsForValue().set(activeKey, tokenHash, expiration);
                });
                return results != null && results.size() == 3;
            } finally {
                operations.unwatch();
            }
        });
        // 4. rechazar códigos inválidos, vencidos o ya consumidos
        if (!Boolean.TRUE.equals(verified)) {
            throw new InvalidRecoveryCode("El código es inválido o ha expirado");
        }
        return resetToken;
    }

    // ---------- consumo de tokens ----------
    @Override
    public String consumeResetToken(String resetToken) {
        // 1. rechazar tokens ausentes o vacíos
        if (resetToken == null || resetToken.isBlank()) {
            throw new InvalidResetToken();
        }
        // 2. obtener la clave redis del token
        String tokenHash = digest(resetToken);
        String tokenKey = TOKEN_PREFIX + tokenHash;

        // 3. obtener el correo asociado al token y comprobar su existencia
        String email = cache.get(tokenKey).orElse(null);
        if (email == null) {
            throw new InvalidResetToken();
        }
        // 4. validar y consumir el token y su referencia activa en una transacción
        String consumedEmail = transactions.execute(operations -> {
            String activeKey = ACTIVE_PREFIX + reference(email, CodeType.RECOVERY);
            operations.watch(List.of(tokenKey, activeKey));
            try {
                if (!email.equals(operations.opsForValue().get(tokenKey))
                        || !tokenHash.equals(operations.opsForValue().get(activeKey))) {
                    return null;
                }
                List<Object> results = transactions.commit(operations,
                        () -> operations.delete(List.of(tokenKey, activeKey)));
                return results != null && results.size() == 1
                        && Long.valueOf(2).equals(results.getFirst()) ? email : null;
            } finally {
                operations.unwatch();
            }
        });
        // 5. rechazar tokens vencidos o previamente utilizados
        if (consumedEmail == null) {
            throw new InvalidResetToken();
        }
        return consumedEmail;
    }

    // --- helpers ---
    private static String reference(String email, CodeType type) {
        return digest(type.name() + ":" + email);
    }

    private static long seconds(long minutes) {
        if (minutes <= 0) {
            throw new IllegalStateException("La expiración debe ser mayor que cero");
        }
        return Math.multiplyExact(minutes, 60);
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
