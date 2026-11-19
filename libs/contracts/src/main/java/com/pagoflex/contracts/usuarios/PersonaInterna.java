package com.pagoflex.contracts.usuarios;

import java.util.UUID;

/// Datos mínimos de una persona para otros servicios; usuarioId es nulo si no tiene cuenta.
public record PersonaInterna(
        UUID id,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        UUID usuarioId
) {
}
