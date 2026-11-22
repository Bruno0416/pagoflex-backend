package com.pagoflex.usuarios.features.auth.dto.response;

import com.pagoflex.usuarios.core.security.Role;

public record AuthResponse(String tokenAcceso, String tokenRenovacion, Role rol) {}
