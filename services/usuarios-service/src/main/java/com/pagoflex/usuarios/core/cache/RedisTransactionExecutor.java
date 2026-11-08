package com.pagoflex.usuarios.core.cache;

import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Component
public class RedisTransactionExecutor {
    private final StringRedisTemplate redis;

    public RedisTransactionExecutor(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    // ---------- transacciones redis ----------
    public <T> T execute(Function<RedisOperations<String, String>, T> action) {
        Objects.requireNonNull(action);
        return redis.execute(new SessionCallback<T>() {
            @Override
            @SuppressWarnings("unchecked")
            public <K, V> T execute(RedisOperations<K, V> operations) {
                return action.apply((RedisOperations<String, String>) operations);
            }
        });
    }

    public List<Object> commit(RedisOperations<String, String> operations, Runnable changes) {
        Objects.requireNonNull(operations);
        Objects.requireNonNull(changes);

        // 1. agrupar los cambios en una transacción
        operations.multi();
        try {
            changes.run();

            // 2. confirmar o abortar si watch detecto una modificación
            return operations.exec();

        } catch (RuntimeException ex) {
            // 2. descartar la transacción ante un error local
            try {
                operations.discard();
            } catch (RuntimeException cleanupError) {
                ex.addSuppressed(cleanupError);
            }
            throw ex;
        }
    }
}
