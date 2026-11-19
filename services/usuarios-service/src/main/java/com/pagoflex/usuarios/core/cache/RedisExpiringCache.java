package com.pagoflex.usuarios.core.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

// lectura de valores que redis expira por si mismo (codigos y tokens temporales)
@Service
public class RedisExpiringCache implements ExpiringCache {
    private final StringRedisTemplate redis;

    public RedisExpiringCache(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    @Override
    public Optional<String> get(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("La clave del cache no puede estar vacia");
        }
        return Optional.ofNullable(redis.opsForValue().get(key));
    }
}
