package com.pagoflex.contracts.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/// Persona sin cuenta que incorpora un ejecutivo al registrar un compromiso (RN-20).
public record CrearPersonaInterna(
        @NotBlank @Size(max = 12) String rut,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @NotBlank @Size(max = 20) String telefono,
        @Email @Size(max = 150) String correo
) {
}
