package com.pagoflex.usuarios.core.cache;

import java.util.Optional;

public interface ExpiringCache {
    Optional<String> get(String key);
}
