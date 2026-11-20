package com.pagoflex.contracts.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/// Código privado de registro: AA00AA (identificador público) + "-" + 26 caracteres Base32 (128 bits aleatorios).
public record ValidateOrganizationCodeRequest(
        @NotBlank(message = "El codigo de empresa es obligatorio")
        @Pattern(regexp = FORMATO, message = "El codigo no tiene un formato valido")
        String codigo
) {
    public static final String FORMATO = "^[A-Z]{2}[0-9]{2}[A-Z]{2}-[A-Z2-7]{26}$";

    public static boolean esValido(String codigo) {
        return codigo != null && codigo.matches(FORMATO);
    }
}
