package com.pagoflex.cobranza.core.security;

import com.pagoflex.security.InternalApiKeyFilter;
import com.pagoflex.security.JwtSupport;
import com.pagoflex.security.PublicPathsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PublicPathsProperties.class)
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) throws Exception {
        // 1. las rutas internas solo aceptan la clave compartida entre servicios, nunca un jwt
        http.securityMatcher("/api/v1/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalApiKeyFilter(internalKey), BearerTokenAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PublicPathsProperties publicPaths,
            @Value("${pagoflex.security.jwt.secret}") String secret,
            StringRedisTemplate redis) throws Exception {
        // 1. desactivar csrf y sesiones
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. cada perfil accede solo a sus rutas
                .authorizeHttpRequests(auth -> auth.requestMatchers(publicPaths.matchers()).permitAll()
                        .requestMatchers("/api/v1/compromisos/me", "/api/v1/compromisos/me/**",
                                "/api/v1/pagos/me", "/api/v1/comprobantes/*",
                                "/api/v1/anulaciones/me", "/api/v1/cumplimiento/me").hasRole("USUARIO_FINAL")
                        .requestMatchers("/api/v1/empresas/me", "/api/v1/empresas/me/**").hasRole("EJECUTIVO")
                        .anyRequest().denyAll())
                // 3. validar el jwt y su sesion en redis, igual que usuarios-service
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret, redis))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
