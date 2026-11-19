package com.pagoflex.cobranza.features.compromiso.dto.response;

/// al registrar: si la persona ya existe no se le piden los datos de nuevo RN-20
public record PersonaEncontradaResponse(String nombre, String correo) {
}
