package com.pagoflex.usuarios.features.internal.dto.response;

import java.util.UUID;

/// Persona para otros servicios; idUsuario, correoCuenta e idEmpresa solo vienen si la persona tiene cuenta o empresa.
public record PersonResponse(UUID idPersona, String rut, String nombres, String apellidos,
                             String correoContacto, String telefono, UUID idUsuario, String correoCuenta,
                             UUID idEmpresa) {

    // persona sin cuenta
    public PersonResponse(UUID idPersona, String rut, String nombres, String apellidos,
                          String correoContacto, String telefono) {
        this(idPersona, rut, nombres, apellidos, correoContacto, telefono, null, null, null);
    }
}
