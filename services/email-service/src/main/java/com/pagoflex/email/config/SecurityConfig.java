package com.pagoflex.email.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Configuration
public class SecurityConfig {
    private static final String INTERNAL_PATH = "/api/v1/email/internal/send";

    @Bean
    SecurityFilterChain security(HttpSecurity http,
                                 @Value("${pagoflex.security.internal-api-key}") String internalKey) throws Exception {
        if (internalKey.isBlank()) throw new IllegalArgumentException("INTERNAL_API_KEY es obligatorio");

        // 1. crear el filtro para validar la clave interna
        OncePerRequestFilter internalKeyFilter = new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain) throws ServletException, IOException {
                // 1. validar la clave al solicitar un envio interno
                if (INTERNAL_PATH.equals(request.getRequestURI()) && "POST".equals(request.getMethod())) {
                    String provided = request.getHeader("X-Internal-Api-Key");
                    if (provided == null || !MessageDigest.isEqual(
                            internalKey.getBytes(StandardCharsets.UTF_8),
                            provided.getBytes(StandardCharsets.UTF_8))) {
                        // 1. rechazar claves ausentes o incorrectas
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                }
                // 2. continuar con el filtro
                chain.doFilter(request, response);
            }
        };

        // 2. permitir health y proteger el endpoint interno
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST, INTERNAL_PATH).permitAll()
                        .anyRequest().denyAll())
                .addFilterBefore(internalKeyFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
