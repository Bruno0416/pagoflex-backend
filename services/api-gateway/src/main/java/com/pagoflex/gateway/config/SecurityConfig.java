package com.pagoflex.gateway.config;

import com.pagoflex.security.JwtSupport;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.pagoflex.security.PublicPathsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableConfigurationProperties(PublicPathsProperties.class)
public class SecurityConfig {
    // ---------- seguridad del gateway ----------
    @Bean
    SecurityFilterChain security(
            HttpSecurity http,
            PublicPathsProperties publicPaths,
            @Value("${pagoflex.security.jwt.secret}") String secret,
            StringRedisTemplate redis) throws Exception {
        // 1. desactivar sesiones y csrf
        http.csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. bloquear toda ruta interna y autorizar las publicas configuradas
                .authorizeHttpRequests(auth -> auth.requestMatchers("/api/v1/usuarios/internal/**", "/api/v1/internal/**").denyAll()
                        .requestMatchers(publicPaths.matchers()).permitAll()
                        .anyRequest().authenticated())
                // 3. validar el jwt en las demas rutas
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret, redis))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
