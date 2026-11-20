package com.pagoflex.soporte.core.client;

import java.util.UUID;

/// Persona como la entrega usuarios-service; usuarioId y empresaId son nulos si no aplican
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
}
