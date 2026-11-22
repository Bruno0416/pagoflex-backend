package com.pagoflex.soporte.core.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class ClockConfig {

    // reloj con la zona de chile; en los tests se reemplaza por uno fijo
    @Bean
    public Clock clock(@Value("${pagoflex.zona-horaria:America/Santiago}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
