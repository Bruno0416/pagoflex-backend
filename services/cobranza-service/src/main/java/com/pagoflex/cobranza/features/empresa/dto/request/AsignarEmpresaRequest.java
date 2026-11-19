package com.pagoflex.cobranza.features.empresa.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;


public record AsignarEmpresaRequest(
        @NotBlank(message = "El correo del ejecutivo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        String correo,

        @NotBlank(message = "El código de la empresa es obligatorio")
        String codigoEmpresa
) {
}
