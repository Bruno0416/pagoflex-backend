package com.pagoflex.cobranza.support;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Redis falso para las pruebas: responde con las sesiones de los tokens firmados en TokensPrueba
@Configuration
public class RedisPrueba {

    @Bean
    @Primary
    @SuppressWarnings("unchecked")
    public StringRedisTemplate redisDePrueba() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valores = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(valores);
        when(valores.get(anyString())).thenAnswer(i -> TokensPrueba.SESIONES.get(i.<String>getArgument(0)));
        return redis;
    }
}
