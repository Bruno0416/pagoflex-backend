package com.pagoflex.contracts.organization;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ValidateOrganizationCodeRequest(
        @NotBlank(message = "El codigo de empresa es obligatorio")
        @Pattern(regexp = FORMATO, message = "El codigo debe tener el formato AA00AA")
        String codigo
) {
    public static final String FORMATO = "^[A-Z]{2}[0-9]{2}[A-Z]{2}$";

    public static boolean esValido(String codigo) {
        return codigo != null && codigo.matches(FORMATO);
    }
}
