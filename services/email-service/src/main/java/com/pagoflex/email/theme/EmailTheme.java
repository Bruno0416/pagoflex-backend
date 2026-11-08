package com.pagoflex.email.theme;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Tokens de color para las plantillas de correo, leídos una sola vez de {@code email-theme.json}.
/// Expone, por plantilla, {@code {light: {alias: "#HEX"}, dark: {...}}} ya resueltos.

public final class EmailTheme {
    private static final String RESOURCE = "email-theme.json";
    private static final String[] SCHEMES = {"light", "dark"};
    private static final Pattern REF = Pattern.compile("^\\{(\\w+)}$");

    private static final Map<String, Map<String, String>> BASE;
    private static final Map<String, Map<String, Map<String, String>>> TEMPLATES;

    static {
        JsonNode root;
        try (InputStream in = EmailTheme.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + RESOURCE + " en el classpath");
            }
            root = JsonMapper.builder().build().readTree(in);
        } catch (IOException ex) {
            throw new IllegalStateException("No fue posible leer " + RESOURCE, ex);
        }

        Map<String, Map<String, String>> base = new HashMap<>();
        for (String scheme : SCHEMES) {
            Map<String, String> identity = flatten(root.path("identity").path(scheme));
            base.put(scheme, resolve(root.path("email").path(scheme), identity, scheme));
        }

        Map<String, Map<String, Map<String, String>>> templates = new HashMap<>();
        root.path("templates").properties().forEach(entry -> {
            Map<String, Map<String, String>> perScheme = new HashMap<>();
            for (String scheme : SCHEMES) {
                Map<String, String> scope = new LinkedHashMap<>(flatten(root.path("identity").path(scheme)));
                scope.putAll(base.get(scheme));
                Map<String, String> merged = new LinkedHashMap<>(base.get(scheme));
                merged.putAll(resolve(entry.getValue().path(scheme), scope, entry.getKey() + "." + scheme));
                perScheme.put(scheme, Map.copyOf(merged));
            }
            templates.put(entry.getKey(), Map.copyOf(perScheme));
        });

        BASE = Map.copyOf(base);
        TEMPLATES = Map.copyOf(templates);
    }

    private EmailTheme() {
    }

    /// Colores resueltos de la plantilla; si no tiene alias propios, solo los compartidos.
    public static Map<String, Map<String, String>> forTemplate(String templateName) {
        return TEMPLATES.getOrDefault(templateName, BASE);
    }

    private static Map<String, String> flatten(JsonNode node) {
        Map<String, String> out = new LinkedHashMap<>();
        node.properties().forEach(e -> out.put(e.getKey(), e.getValue().asString()));
        return out;
    }

    private static Map<String, String> resolve(JsonNode aliases, Map<String, String> scope, String where) {
        Map<String, String> out = new LinkedHashMap<>();
        aliases.properties().forEach(e -> {
            String value = e.getValue().asString();
            Matcher m = REF.matcher(value);
            if (m.matches()) {
                value = scope.get(m.group(1));
                if (value == null) {
                    throw new IllegalStateException("Token sin resolver en " + RESOURCE + " (" + where + "): "
                            + e.getKey() + " -> " + e.getValue().asString());
                }
            }
            out.put(e.getKey(), value);
        });
        return out;
    }
}
