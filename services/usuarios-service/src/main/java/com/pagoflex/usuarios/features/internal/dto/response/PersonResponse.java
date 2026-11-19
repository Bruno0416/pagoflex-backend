package com.pagoflex.usuarios.features.internal.dto.response;

import java.util.UUID;

public record PersonResponse(UUID idPersona, String rut, String nombres, String apellidos,
                             String correoContacto, String telefono) {}
