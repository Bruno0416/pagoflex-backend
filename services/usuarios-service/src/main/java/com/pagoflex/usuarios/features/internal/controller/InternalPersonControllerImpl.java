package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.contracts.usuarios.SolicitudCrearPersonaInterna;
import com.pagoflex.contracts.usuarios.RespuestaPersonaInterna;
import com.pagoflex.usuarios.features.internal.service.InternalPersonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usuarios/internal/personas")
@RequiredArgsConstructor
public class InternalPersonControllerImpl implements InternalPersonController {
    private final InternalPersonService service;

    @Override
    public ResponseEntity<RespuestaPersonaInterna> create(@Valid @RequestBody SolicitudCrearPersonaInterna request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @Override
    public ResponseEntity<RespuestaPersonaInterna> byRut(@PathVariable String rut) {
        return ResponseEntity.ok(service.byRut(rut));
    }

    @Override
    public ResponseEntity<RespuestaPersonaInterna> byUserId(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(service.byUserId(idUsuario));
    }

    @Override
    public ResponseEntity<RespuestaPersonaInterna> byEmail(@PathVariable String correo) {
        return ResponseEntity.ok(service.byEmail(correo));
    }

    @Override
    public ResponseEntity<RespuestaPersonaInterna> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.byId(id));
    }
}
