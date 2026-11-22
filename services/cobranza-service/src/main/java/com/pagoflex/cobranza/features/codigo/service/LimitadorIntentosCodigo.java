package com.pagoflex.cobranza.features.codigo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/// Corta los intentos fallidos contra un mismo prefijo de código: tras 5 fallos se bloquea 15 minutos.
/// Si redis no responde se bloquea (falla cerrada), igual que la validación de sesiones.
@Component
@RequiredArgsConstructor
public class LimitadorIntentosCodigo {
    static final int MAXIMO_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(15);
    private static final String PREFIJO = "pagoflex:cobranza:codigo-fallos:";

    private final StringRedisTemplate redis;

    public boolean bloqueado(String prefijo) {
        try {
            String valor = redis.opsForValue().get(PREFIJO + prefijo);
            return valor != null && Long.parseLong(valor) >= MAXIMO_FALLOS;
        } catch (RuntimeException ex) {
            return true;
        }
    }

    public void registrarFallo(String prefijo) {
        try {
            Long total = redis.opsForValue().increment(PREFIJO + prefijo);
            // la ventana empieza con el primer fallo
            if (total != null && total == 1L) redis.expire(PREFIJO + prefijo, VENTANA);
        } catch (RuntimeException ex) {
            // sin redis ya se bloquea en bloqueado()
        }
    }

    public void limpiar(String prefijo) {
        try {
            redis.delete(PREFIJO + prefijo);
        } catch (RuntimeException ex) {
            // el contador vence solo
        }
    }
}
