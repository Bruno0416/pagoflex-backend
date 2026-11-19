package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.usuarios.features.internal.service.PersonaInternaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Rutas para otros servicios (cobranza y soporte); exigen X-Internal-Api-Key.
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class PersonaInternaController {
    private final PersonaInternaService service;

    @GetMapping("/personas/{id}")
    public PersonaInterna porId(@PathVariable UUID id) {
        return service.porId(id);
    }

    @GetMapping("/personas/rut/{rut}")
    public PersonaInterna porRut(@PathVariable String rut) {
        return service.porRut(rut);
    }

    @GetMapping("/personas/usuario/{usuarioId}")
    public PersonaInterna porUsuario(@PathVariable UUID usuarioId) {
        return service.porUsuario(usuarioId);
    }

    @GetMapping("/usuarios/correo/{correo}")
    public PersonaInterna porCorreo(@PathVariable String correo) {
        return service.porCorreo(correo);
    }

    @PostMapping("/personas")
    @ResponseStatus(HttpStatus.CREATED)
    public PersonaInterna crear(@Valid @RequestBody CrearPersonaInterna request) {
        return service.crear(request);
    }
}
