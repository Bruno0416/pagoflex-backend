package com.pagoflex.usuarios.core.security;

import com.pagoflex.usuarios.features.auth.entity.Users;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {
    private final SecretKey key;
    private final Duration expiration;

    public JwtUtil(
            @Value("${pagoflex.security.jwt.secret}") String secret,
            @Value("${pagoflex.security.jwt.expiration}") Duration expiration) {
        // 1. verificar la longitud del secreto
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes UTF-8");


        // 2. configurar la firma y la expiración
        this.key = Keys.hmacShaKeyFor(bytes);
        this.expiration = expiration;
    }

    public String generateToken(Users user) {
        // 1. establecer identidad y atributos del usuario
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("token_type", "ACCESS")
                .claim("userId", user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", List.of(user.getRole().name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                // 2. firmar el token de acceso
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }
}
