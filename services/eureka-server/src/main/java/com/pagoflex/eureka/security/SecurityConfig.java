package com.pagoflex.eureka.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // ---------- acceso interno de eureka ----------
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        // 1. definir las rutas protegidas
        http.securityMatcher("/eureka/**")
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                // 2. configurar la autenticación básica
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    // ---------- acceso al panel web ----------
    @Bean
    @Order(2)
    public SecurityFilterChain uiFilterChain(HttpSecurity http) throws Exception {
        // 1. proteger el acceso al panel
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(
                        auth -> auth.requestMatchers("/actuator/**").permitAll().anyRequest().authenticated())
                // 2. configurar el formulario de acceso
                .formLogin(Customizer.withDefaults());

        return http.build();
    }
}
