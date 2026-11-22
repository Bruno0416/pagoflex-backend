package com.pagoflex.cobranza.features.codigo;

import com.pagoflex.cobranza.features.codigo.service.LimitadorIntentosCodigo;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class LimitadorIntentosCodigoTest {
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> ops = mock(ValueOperations.class);
    private final LimitadorIntentosCodigo limitador = new LimitadorIntentosCodigo(redis);

    LimitadorIntentosCodigoTest() {
        when(redis.opsForValue()).thenReturn(ops);
    }

    @Test
    void bloqueaTrasCincoFallosDelMismoPrefijo() {
        when(ops.get("pagoflex:cobranza:codigo-fallos:AB12CD")).thenReturn("4");
        assertThat(limitador.bloqueado("AB12CD")).isFalse();
        when(ops.get("pagoflex:cobranza:codigo-fallos:AB12CD")).thenReturn("5");
        assertThat(limitador.bloqueado("AB12CD")).isTrue();
        when(ops.get("pagoflex:cobranza:codigo-fallos:ZZ99ZZ")).thenReturn(null);
        assertThat(limitador.bloqueado("ZZ99ZZ")).isFalse();
    }

    @Test
    void laVentanaEmpiezaConElPrimerFallo() {
        when(ops.increment("pagoflex:cobranza:codigo-fallos:AB12CD")).thenReturn(1L, 2L);

        limitador.registrarFallo("AB12CD");
        limitador.registrarFallo("AB12CD");

        verify(redis, times(1)).expire("pagoflex:cobranza:codigo-fallos:AB12CD", Duration.ofMinutes(15));
    }

    @Test
    void sinRedisFallaCerrado() {
        when(ops.get(anyString())).thenThrow(new IllegalStateException("redis caído"));

        assertThat(limitador.bloqueado("AB12CD")).isTrue();
        // registrar y limpiar no propagan el error
        when(ops.increment(anyString())).thenThrow(new IllegalStateException("redis caído"));
        limitador.registrarFallo("AB12CD");
        doThrow(new IllegalStateException("redis caído")).when(redis).delete(anyString());
        limitador.limpiar("AB12CD");
    }
}
