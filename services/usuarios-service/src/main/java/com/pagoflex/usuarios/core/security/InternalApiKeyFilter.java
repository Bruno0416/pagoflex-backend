package com.pagoflex.usuarios.core.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/// Protege las rutas internas: solo otros servicios con la clave compartida pueden llamarlas.
public class InternalApiKeyFilter extends OncePerRequestFilter {
    public static final String INTERNAL_PATHS = "/api/v1/internal/**";
    private static final String INTERNAL_PREFIX = "/api/v1/internal/";

    private final byte[] internalKey;

    public InternalApiKeyFilter(String internalKey) {
        if (internalKey == null || internalKey.isBlank()) {
            throw new IllegalArgumentException("INTERNAL_API_KEY es obligatorio");
        }
        this.internalKey = internalKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 1. validar la clave solo en las rutas internas
        if (request.getRequestURI().startsWith(INTERNAL_PREFIX)) {
            String provided = request.getHeader("X-Internal-Api-Key");
            if (provided == null || !MessageDigest.isEqual(internalKey, provided.getBytes(StandardCharsets.UTF_8))) {
                // 1. rechazar claves ausentes o incorrectas
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        // 2. continuar con el filtro
        chain.doFilter(request, response);
    }
}
