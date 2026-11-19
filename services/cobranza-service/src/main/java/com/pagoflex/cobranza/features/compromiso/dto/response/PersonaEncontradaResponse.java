package com.pagoflex.cobranza.features.compromiso.dto.response;

/// persona ya registrada: al ejecutivo solo le sirve para no pedir sus datos otra vez RN-20
public record PersonaEncontradaResponse(String nombre, String correo) {
}
