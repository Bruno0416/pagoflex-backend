package com.pagoflex.reconocimientos.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.time.ZoneId;

// reloj controlable para las pruebas; se importa de forma explicita desde IntegrationTest
@TestConfiguration
public class TestClockConfig {
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(Instant.parse(IntegrationTest.DEFAULT_NOW), ZoneId.of("America/Santiago"));
    }
}
