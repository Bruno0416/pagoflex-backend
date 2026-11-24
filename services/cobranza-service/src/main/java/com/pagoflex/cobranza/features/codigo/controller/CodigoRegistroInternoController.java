package com.pagoflex.cobranza.features.codigo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.features.codigo.service.CodigoRegistroService;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import com.pagoflex.contracts.organizacion.RespuestaValidacionCodigoEmpresa;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Validación del código de registro para usuarios-service; exige X-Internal-Api-Key
@RestController
@Tag(name = "Interno: códigos de registro")
@SecurityRequirement(name = "internalApiKey")
@RequestMapping("/api/v1/internal/empresas/codigos")
@RequiredArgsConstructor
public class CodigoRegistroInternoController {
    private final CodigoRegistroService service;

    @Operation(summary = "Validar el código de empresa al registrar agentes y ejecutivos")
    @PostMapping("/validar")
    public ResponseEntity<RespuestaValidacionCodigoEmpresa> validar(
            @Valid @RequestBody SolicitudValidacionCodigoEmpresa request) {
        return ResponseEntity.ok(service.validar(request.codigo()));
    }
}
