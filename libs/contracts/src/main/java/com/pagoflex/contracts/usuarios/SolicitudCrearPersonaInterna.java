package com.pagoflex.contracts.usuarios;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud interna para registrar una persona sin crear una cuenta.
 * El correo de contacto es opcional; el teléfono es obligatorio.
 */
public record SolicitudCrearPersonaInterna(
        @NotBlank
        @Pattern(regexp = "^\\d{7,8}-[0-9kK]$", message = "Formato RUT invalido")
        String rut,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @Email @Size(max = 150) String correoContacto,
        @NotBlank
        @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Telefono invalido")
        String telefono
) {}
