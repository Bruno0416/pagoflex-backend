package com.pagoflex.contracts.organizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


public record SolicitudValidacionCodigoEmpresa(
        @NotBlank(message = "El codigo de empresa es obligatorio")
        @Pattern(regexp = FORMATO, message = "El codigo no tiene un formato valido")
        String codigo
) {
    public static final String FORMATO = "^[A-Z]{2}[0-9]{2}[A-Z]{2}-[A-Z2-7]{26}$";

    public static boolean esValido(String codigo) {
        return codigo != null && codigo.matches(FORMATO);
    }
}
