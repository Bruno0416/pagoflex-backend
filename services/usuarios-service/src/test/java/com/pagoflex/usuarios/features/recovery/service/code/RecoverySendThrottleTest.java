package com.pagoflex.usuarios.features.recovery.service.code;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoverySendThrottleTest {
    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> values;

    @Test
    void allowsOneRequestPerMinuteWithoutStoringPlainEmailInKey() {
        when(redis.opsForValue()).thenReturn(values);
        when(values.setIfAbsent(anyString(), eq("1"), eq(Duration.ofSeconds(60))))
                .thenReturn(true, false);
        var throttle = new RecoverySendThrottle(redis);

        assertThat(throttle.allow("usuario@example.com")).isTrue();
        assertThat(throttle.allow("usuario@example.com")).isFalse();
        verify(values, times(2)).setIfAbsent(
                matches("pagoflex:auth:send-cooldown:[0-9a-f]{64}"), eq("1"),
                eq(Duration.ofSeconds(60)));
    }
}
