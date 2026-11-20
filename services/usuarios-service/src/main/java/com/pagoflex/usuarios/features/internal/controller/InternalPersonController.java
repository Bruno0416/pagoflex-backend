package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.usuarios.features.internal.dto.request.CreatePersonRequest;
import com.pagoflex.usuarios.features.internal.dto.response.AccountPersonResponse;
import com.pagoflex.usuarios.features.internal.dto.response.PersonResponse;
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
public class InternalPersonController {
    private final InternalPersonService service;

    @PostMapping
    public ResponseEntity<PersonResponse> create(@Valid @RequestBody CreatePersonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/por-rut/{rut}")
    public ResponseEntity<PersonResponse> byRut(@PathVariable String rut) {
        return ResponseEntity.ok(service.byRut(rut));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PersonResponse> byId(@PathVariable UUID id) {
        return ResponseEntity.ok(service.byId(id));
    }

    // persona de la cuenta del token (cobranza y soporte)
    @GetMapping("/por-usuario/{idUsuario}")
    public ResponseEntity<AccountPersonResponse> byUser(@PathVariable UUID idUsuario) {
        return ResponseEntity.ok(service.byUser(idUsuario));
    }

    // persona de la cuenta con ese correo (cargas de ejemplo)
    @GetMapping("/por-correo/{correo}")
    public ResponseEntity<AccountPersonResponse> byEmail(@PathVariable String correo) {
        return ResponseEntity.ok(service.byEmail(correo));
    }
}
