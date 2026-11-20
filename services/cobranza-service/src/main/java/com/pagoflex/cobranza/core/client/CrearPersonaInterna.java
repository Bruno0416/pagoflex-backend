package com.pagoflex.cobranza.core.client;

/// Persona sin cuenta que incorpora un ejecutivo al registrar un compromiso (RN-20)
public record CrearPersonaInterna(String rut, String nombres, String apellidos, String telefono, String correo) {
}
