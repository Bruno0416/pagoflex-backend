package com.pagoflex.contracts.correo;

import java.util.Set;

public enum TipoCorreo {
    RECOVERY("Recupera tu contraseña de PagoFlex", "code");

    private final String subject;
    private final Set<String> requiredVariables;

    TipoCorreo(String subject, String... requiredVariables) {
        this.subject = subject;
        this.requiredVariables = Set.of(requiredVariables);
    }

    public String subject() {
        return subject;
    }

    public String templateName() {
        return switch (this) {
            case RECOVERY -> "recovery";
        };
    }

    public Set<String> requiredVariables() {
        return requiredVariables;
    }
}
