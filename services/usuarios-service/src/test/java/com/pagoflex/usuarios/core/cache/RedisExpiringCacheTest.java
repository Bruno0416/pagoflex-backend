package com.pagoflex.usuarios.core.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisExpiringCacheTest {
    @Mock
    private StringRedisTemplate redis;
    @Mock
    private RedisOperations<String, String> operations;
    @Mock
    private ValueOperations<String, String> values;

    private ExpiringCache cache;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setup() {
        cache = new RedisExpiringCache(redis, new RedisTransactionExecutor(redis));
        lenient().when(redis.opsForValue()).thenReturn(values);
        lenient().when(operations.opsForValue()).thenReturn(values);
        lenient().when(redis.execute(any(SessionCallback.class))).thenAnswer(invocation -> {
            SessionCallback<?> callback = invocation.getArgument(0);
            return callback.execute(operations);
        });
    }

    @Test
    void storesWithTtl() {
        cache.put("key", "value", Duration.ofSeconds(30));
        verify(values).set("key", "value", Duration.ofSeconds(30));
    }

    @Test
    void rejectsInvalidTtlAndKey() {
        assertThatThrownBy(() -> cache.put("key", "value", Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cache.put(" ", "value", Duration.ofSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(values);
    }

    @Test
    void readsAndComparesWithoutConsuming() {
        when(values.get("key")).thenReturn("expected");
        assertThat(cache.get("key")).contains("expected");
        assertThat(cache.matches("key", "expected")).isTrue();
        assertThat(cache.matches("key", "other")).isFalse();
        verify(operations, never()).multi();
        verify(redis, never()).delete(anyString());
    }

    @Test
    void consumesOnlyWhenExpectedMatches() {
        when(values.get("key")).thenReturn("expected");
        when(operations.exec()).thenReturn(List.of(true));
        assertThat(cache.consumeIfMatches("key", "expected")).isTrue();
        verify(operations).watch("key");
        verify(operations).multi();
        verify(operations).delete("key");
        verify(operations).exec();
        verify(operations).unwatch();
    }

    @Test
    void doesNotConsumeWrongValue() {
        when(values.get("key")).thenReturn("other");
        assertThat(cache.consumeIfMatches("key", "expected")).isFalse();
        verify(operations, never()).multi();
        verify(operations).unwatch();
    }

    @Test
    void expiredOrAlreadyConsumedValueFailsClosed() {
        assertThat(cache.consumeIfMatches("key", "expected")).isFalse();
        verify(operations, never()).multi();
    }

    @Test
    void competingConsumerInvalidatesWatchAndCannotReuseToken() {
        when(values.get("key")).thenReturn("expected");
        // exec devuelve null cuando otra solicitud ya modifico la clave
        when(operations.exec()).thenReturn(null);
        assertThat(cache.consumeIfMatches("key", "expected")).isFalse();
        verify(operations).exec();
        verify(operations).unwatch();
    }

    @Test
    void deleteReportsWhetherTheKeyExisted() {
        when(redis.delete("key")).thenReturn(true, false);
        assertThat(cache.delete("key")).isTrue();
        assertThat(cache.delete("key")).isFalse();
    }
}
