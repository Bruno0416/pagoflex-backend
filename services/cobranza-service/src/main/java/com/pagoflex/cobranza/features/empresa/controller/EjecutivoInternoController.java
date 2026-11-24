package com.pagoflex.cobranza.features.empresa.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Empresa de cada ejecutivo para otros servicios; exigen X-Internal-Api-Key
@RestController
@Tag(name = "Interno: empresa del ejecutivo")
@SecurityRequirement(name = "internalApiKey")
@RequestMapping("/api/v1/internal/empresas/ejecutivos")
@RequiredArgsConstructor
public class EjecutivoInternoController {
    private final EjecutivoService service;

    @Operation(summary = "Empresa a la que pertenece un ejecutivo")
    @GetMapping("/{usuarioId}")
    public EmpresaInterna empresa(@PathVariable UUID usuarioId) {
        return service.interna(usuarioId);
    }
}
