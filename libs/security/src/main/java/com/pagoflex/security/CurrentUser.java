package com.pagoflex.security;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Set;
import java.util.UUID;


/// Acceso a la identidad de la petición autenticada.
/// No válida ni decodifica tokens: eso corresponde al filtro JWT de Spring Security.
public final class CurrentUser {
    private CurrentUser() {
    }

    public static AuthenticatedUser get() {
        Jwt jwt = jwt();
        UUID id;
        try {
            id = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new BadCredentialsException("JWT sin UUID de usuario valido", ex);
        }

        var roles = jwt.getClaimAsStringList("roles");
        if (roles == null || roles.isEmpty() || roles.stream().anyMatch(role -> role == null || role.isBlank())) {
            throw new BadCredentialsException("JWT sin roles validos");
        }

        return new AuthenticatedUser(id, jwt.getClaimAsString("email"), Set.copyOf(roles));
    }

    public static UUID id() {
        return get().id();
    }

    public static boolean hasRole(String role) {
        return get().hasRole(role);
    }

    /**
     * Acceso a claims adicionales del JWT previamente verificado.
     */
    public static Jwt jwt() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth) || !authentication.isAuthenticated()) {
            throw new AuthenticationCredentialsNotFoundException("No hay un usuario JWT autenticado");
        }
        return jwtAuth.getToken();
    }
}
