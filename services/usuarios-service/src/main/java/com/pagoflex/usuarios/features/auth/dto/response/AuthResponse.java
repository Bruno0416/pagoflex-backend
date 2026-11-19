package com.pagoflex.usuarios.features.auth.dto.response;

public record AuthResponse(String tokenAcceso, String tokenRenovacion, String tipoToken, long expiraEnSegundos) {}
