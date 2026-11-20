package com.pagoflex.cobranza.core.security;

import com.pagoflex.security.JwtSupport;
import com.pagoflex.security.PublicPathsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            PublicPathsProperties publicPaths,
            @Value("${pagoflex.security.jwt.secret}") String secret,
            StringRedisTemplate redis,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) throws Exception {
        // 1. desactivar csrf y sesiones
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. rutas públicas, internas con clave y cada perfil con las suyas
                .authorizeHttpRequests(auth -> auth.requestMatchers(publicPaths.matchers()).permitAll()
                        .requestMatchers(InternalApiKeyFilter.INTERNAL_PATHS).permitAll()
                        .requestMatchers("/api/v1/compromisos/me", "/api/v1/compromisos/me/**",
                                "/api/v1/pagos/me", "/api/v1/comprobantes/*",
                                "/api/v1/anulaciones/me", "/api/v1/cumplimiento/me").hasRole("USUARIO_FINAL")
                        .requestMatchers("/api/v1/empresas/me", "/api/v1/empresas/me/**").hasRole("EJECUTIVO")
                        .anyRequest().denyAll())
                // 3. validar la clave interna antes que el jwt
                .addFilterBefore(new InternalApiKeyFilter(internalKey), BearerTokenAuthenticationFilter.class)
                // 4. validar el jwt en las rutas privadas
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret, redis))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
