package com.pagoflex.usuarios.core.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class RedisExpiringCache implements ExpiringCache {
    private final StringRedisTemplate redis;
    private final RedisTransactionExecutor transactions;

    public RedisExpiringCache(StringRedisTemplate redis, RedisTransactionExecutor transactions) {
        this.redis = Objects.requireNonNull(redis);
        this.transactions = Objects.requireNonNull(transactions);
    }

    // ---------- operaciones de cache ----------
    @Override
    public void put(String key, String value, Duration ttl) {
        validateKey(key);
        Objects.requireNonNull(value, "value");
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("La duración del cache debe ser mayor que cero");
        }
        redis.opsForValue().set(key, value, ttl);
    }

    @Override
    public Optional<String> get(String key) {
        validateKey(key);
        return Optional.ofNullable(redis.opsForValue().get(key));
    }

    @Override
    public boolean matches(String key, String expected) {
        Objects.requireNonNull(expected, "expected");
        return get(key).map(stored -> equalValues(stored, expected)).orElse(false);
    }

    @Override
    public boolean consumeIfMatches(String key, String expected) {
        validateKey(key);
        Objects.requireNonNull(expected, "expected");

        // 1. comparar y eliminar dentro de una misma sesión redis
        return Boolean.TRUE.equals(transactions.execute(operations -> {
            operations.watch(key);
            try {
                String stored = operations.opsForValue().get(key);
                if (stored == null || !equalValues(stored, expected)) {
                    return false;
                }
                // 2. permitir solo al primer consumidor confirmar la eliminación
                List<Object> results = transactions.commit(operations, () -> operations.delete(key));
                return results != null && results.size() == 1 && Boolean.TRUE.equals(results.getFirst());
            } finally {
                operations.unwatch();
            }
        }));
    }

    @Override
    public boolean delete(String key) {
        validateKey(key);
        return Boolean.TRUE.equals(redis.delete(key));
    }

    // --- helpers ---
    private static void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave del cache no puede estar vacia");
        }
    }

    private static boolean equalValues(String stored, String expected) {
        return MessageDigest.isEqual(
                stored.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}
