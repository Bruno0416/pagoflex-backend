package com.pagoflex.usuarios.features.profile.dto.response;

import java.util.UUID;

public record ProfileResponse(
        UUID id,
        UUID idPersona,
        String rut,
        String nombres,
        String apellidos,
        String correo,
        String telefono,
        String rol,
        String urlAvatar,
        UUID idEmpresa
) {}
