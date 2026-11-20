package com.pagoflex.reconocimientos.core.security;

import com.pagoflex.security.InternalApiKeyFilter;
import com.pagoflex.security.JwtSupport;
import com.pagoflex.security.PagoFlexRole;
import com.pagoflex.security.PublicPathsProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(PublicPathsProperties.class)
public class SecurityConfig {
    private static final String UF = PagoFlexRole.USUARIO_FINAL.name();

    // ---------- rutas internas ----------
    @Bean
    @Order(1)
    public SecurityFilterChain internalSecurityFilterChain(
            HttpSecurity http,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) throws Exception {
        // 1. no aceptar jwt como sustituto de la clave de servicio
        http.securityMatcher("/api/v1/internal/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(new InternalApiKeyFilter(internalKey), UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    // ---------- rutas publicas y privadas ----------
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
                // 2. autorizar por ruta y rol; el orden importa (me antes del comodin)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicPaths.matchers()).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/reconocimientos/me").hasRole(UF)
                        .anyRequest().denyAll())
                // 3. validar el jwt y la sesion de redis
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt
                        .decoder(JwtSupport.accessDecoder(secret, redis))
                        .jwtAuthenticationConverter(JwtSupport.authenticationConverter())));
        return http.build();
    }
}
