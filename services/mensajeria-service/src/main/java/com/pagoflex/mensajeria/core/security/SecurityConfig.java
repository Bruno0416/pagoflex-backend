package com.pagoflex.mensajeria.core.security;

import com.pagoflex.security.JwtSupport;
import com.pagoflex.security.PublicPathsProperties;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PublicPathsProperties.class)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PublicPathsProperties publicPaths,
            @Value("${pagoflex.security.jwt.secret}") String secret) throws Exception {
        // 1. desactivar csrf y sesiones
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. solo conversan agentes y ejecutivos; el sse sigue en un hilo asíncrono ya autorizado
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                        .requestMatchers(publicPaths.matchers()).permitAll()
                        .requestMatchers("/api/v1/conversaciones/**").hasAnyRole("AGENTE", "EJECUTIVO")
                        .anyRequest().denyAll())
                // 3. validar el jwt
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
