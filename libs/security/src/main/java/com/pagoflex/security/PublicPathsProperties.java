package com.pagoflex.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/// Rutas que no requieren JWT, declaradas por servicio desde application.yaml
@ConfigurationProperties(prefix = "pagoflex.security")
public record PublicPathsProperties(List<String> publicPaths) {
    public PublicPathsProperties {
        // 1. normalizar la lista de rutas publicas
        publicPaths = publicPaths == null ? List.of() : List.copyOf(publicPaths);
    }

    public String[] matchers() {
        return publicPaths.toArray(String[]::new);
    }
}
