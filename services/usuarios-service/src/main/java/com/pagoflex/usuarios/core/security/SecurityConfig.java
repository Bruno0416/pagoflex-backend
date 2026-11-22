package com.pagoflex.usuarios.core.security;

import com.pagoflex.security.InternalApiKeyFilter;
import com.pagoflex.security.JwtSupport;
import com.pagoflex.security.PublicPathsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PublicPathsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationProvider authenticationProvider;

    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) throws Exception {
        // 1. no aceptar jwt como sustituto de la clave de servicio
        http.securityMatcher("/api/v1/usuarios/internal/**")
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
                .authenticationProvider(authenticationProvider)
                // 2. permitir solo las rutas declaradas en yaml
                .authorizeHttpRequests(auth -> auth.requestMatchers(publicPaths.matchers()).permitAll()
                        .anyRequest().authenticated())
                // 3. validar el jwt en las rutas privadas
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret, redis))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
