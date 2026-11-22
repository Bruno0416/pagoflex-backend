package com.pagoflex.cobranza.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/// Fecha de referencia del caso: 06-10-2026 a las 10:00
@TestConfiguration
public class RelojFijo {
    public static final ZoneId ZONA = ZoneId.of("America/Santiago");
    public static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 6, 10, 0);

    @Bean
    @Primary
    public Clock relojDePrueba() {
        return Clock.fixed(AHORA.atZone(ZONA).toInstant(), ZONA);
    }
}
