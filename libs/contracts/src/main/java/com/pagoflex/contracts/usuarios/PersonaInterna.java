package com.pagoflex.contracts.usuarios;

import java.util.UUID;

/// Datos mínimos de una persona para otros servicios; usuarioId es nulo si no tiene cuenta
/// y empresaId solo existe en cuentas registradas con un código de empresa (ejecutivos y agentes).
public record PersonaInterna(
        UUID id,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        UUID usuarioId,
        UUID empresaId
) {
    public PersonaInterna(UUID id, String rut, String nombres, String apellidos, String correo,
                          String telefono, UUID usuarioId) {
        this(id, rut, nombres, apellidos, correo, telefono, usuarioId, null);
    }
}
