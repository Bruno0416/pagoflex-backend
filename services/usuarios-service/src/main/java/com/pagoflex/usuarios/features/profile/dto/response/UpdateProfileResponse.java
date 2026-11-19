package com.pagoflex.usuarios.features.profile.dto.response;

import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;

public record UpdateProfileResponse(ProfileResponse perfil, AuthResponse sesion) {}
