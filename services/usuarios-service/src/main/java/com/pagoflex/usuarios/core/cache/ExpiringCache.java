package com.pagoflex.usuarios.core.cache;

import java.time.Duration;
import java.util.Optional;

public interface ExpiringCache {
    void put(String key, String value, Duration ttl);

    Optional<String> get(String key);

    boolean matches(String key, String expected);

    boolean consumeIfMatches(String key, String expected);

    boolean delete(String key);
}
