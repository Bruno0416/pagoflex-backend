package com.pagoflex.usuarios.features.internal.service;

import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;

import java.util.UUID;

public interface PersonaInternaService {
    PersonaInterna porId(UUID id);

    PersonaInterna porUsuario(UUID usuarioId);

    PersonaInterna porRut(String rut);

    PersonaInterna porCorreo(String correo);

    PersonaInterna crear(CrearPersonaInterna request);
}
