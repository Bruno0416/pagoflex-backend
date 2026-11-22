package com.pagoflex.contracts.usuarios;

import java.util.UUID;

/**
 * Respuesta HTTP canónica de usuarios-service para otros servicios.
 * Se conservan por separado el correo de contacto y el de cuenta.
 */
public record RespuestaPersonaInterna(
        UUID idPersona,
        String rut,
        String nombres,
        String apellidos,
        String correoContacto,
        String telefono,
        UUID idUsuario,
        String correoCuenta,
        UUID idEmpresa
) {
    public RespuestaPersonaInterna(
            UUID idPersona, String rut, String nombres,
            String apellidos, String correoContacto, String telefono) {
        this(idPersona, rut, nombres, apellidos,
                correoContacto, telefono, null, null, null);
    }

    /**
     * Proyección simplificada usada por Cobranza y Soporte.
     * El correo de acceso tiene prioridad cuando existe una cuenta.
     */
    public PersonaInterna aPersonaInterna() {
        return new PersonaInterna(
                idPersona, rut, nombres, apellidos,
                correoCuenta != null ? correoCuenta : correoContacto,
                telefono, idUsuario, idEmpresa);
    }
}
