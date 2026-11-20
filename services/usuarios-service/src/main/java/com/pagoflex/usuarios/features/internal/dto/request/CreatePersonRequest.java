package com.pagoflex.usuarios.features.internal.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreatePersonRequest(
        @NotBlank @Pattern(regexp = "^\\d{7,8}-[0-9kK]$", message = "Formato RUT invalido") String rut,
        @NotBlank @Size(max = 80) String nombres,
        @NotBlank @Size(max = 80) String apellidos,
        @Email String correoContacto,
        @NotBlank @Pattern(regexp = "^\\+?[0-9]{8,15}$", message = "Telefono invalido") String telefono
) {}
