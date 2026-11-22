package com.pagoflex.cobranza.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// redis simulado: solo responde las sesiones que firmó TokensPrueba
@TestConfiguration
public class RedisPrueba {
    @Bean
    @SuppressWarnings("unchecked")
    public StringRedisTemplate stringRedisTemplate() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.get(anyString())).thenAnswer(i -> TokensPrueba.SESIONES.get(i.<String>getArgument(0)));
        return redis;
    }
}
