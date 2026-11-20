package com.pagoflex.reconocimientos.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {
    // ---------- reloj del servicio ----------
    @Bean
    public Clock clock(@Value("${pagoflex.reconocimientos.zona-horaria}") String zone) {
        // 1. fijar la zona de negocio para meses, vencimientos y cupos
        return Clock.system(ZoneId.of(zone));
    }
}
