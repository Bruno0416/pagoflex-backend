package com.pagoflex.security;

import java.util.Set;
import java.util.UUID;

/// Datos de identidad obtenidos de un JWT de acceso ya autenticado
public record AuthenticatedUser(UUID id, String email, Set<String> roles) {
    public AuthenticatedUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(String role) {
        return roles.contains(role);
    }
}
