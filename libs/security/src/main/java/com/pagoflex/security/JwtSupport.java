package com.pagoflex.security;

import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.nio.charset.StandardCharsets;


/// Validación compartida de JWT; cada servicio define sus propias rutas autorizadas.
public final class JwtSupport {
    private JwtSupport() {
    }

    // ---------- validación de jwt ----------
    public static JwtDecoder accessDecoder(String secret) {
        // 1. validar la longitud del secreto compartido
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes UTF-8");
        }

        // 2. configurar el decodificador hs256
        var key = new javax.crypto.spec.SecretKeySpec(bytes, "HmacSHA256");
        var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        // 3. validar los atributos del token de acceso
        OAuth2TokenValidator<Jwt> access = jwt ->
                "ACCESS".equals(jwt.getClaimAsString("token_type"))
                        && jwt.getSubject() != null
                        && jwt.getExpiresAt() != null
                        && jwt.getClaimAsString("userId") != null
                        && jwt.getSubject().equals(jwt.getClaimAsString("userId"))
                        && jwt.getClaimAsStringList("roles") != null
                        && !jwt.getClaimAsStringList("roles").isEmpty()
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token"));
        // 4. aplicar los validadores de jwt
        decoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), access));
        return decoder;
    }

    public static JwtAuthenticationConverter authenticationConverter() {
        // 1. convertir los roles a autoridades
        var roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");
        var converter = new JwtAuthenticationConverter();
        // 2. identificar al usuario por su id
        converter.setPrincipalClaimName("userId");
        converter.setJwtGrantedAuthoritiesConverter(roles);
        return converter;
    }
}
