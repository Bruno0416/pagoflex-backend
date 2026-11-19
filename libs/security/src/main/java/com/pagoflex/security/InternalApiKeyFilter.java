package com.pagoflex.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class InternalApiKeyFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Internal-Api-Key";

    private final byte[] expected;

    public InternalApiKeyFilter(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("Falta INTERNAL_API_KEY");
        expected = key.getBytes(StandardCharsets.UTF_8);
    }

    // ---------- autorizacion entre servicios ----------
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        // 1. exigir la clave y compararla sin filtraciones de tiempo
        String supplied = request.getHeader(HEADER);
        if (supplied == null || !MessageDigest.isEqual(expected, supplied.getBytes(StandardCharsets.UTF_8))) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        // 2. continuar con la cadena
        chain.doFilter(request, response);
    }
}
