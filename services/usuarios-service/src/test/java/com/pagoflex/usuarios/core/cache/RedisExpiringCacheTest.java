package com.pagoflex.usuarios.core.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisExpiringCacheTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private ValueOperations<String, String> values;

    private ExpiringCache cache;

    @BeforeEach
    void setup() {
        cache = new RedisExpiringCache(redis);
    }

    @Test
    void readsStoredAndMissingValues() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("key")).thenReturn("expected");

        assertThat(cache.get("key")).contains("expected");
        assertThat(cache.get("expired")).isEmpty();
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> cache.get(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.get(null)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(redis);
    }
}
