package com.pagoflex.cobranza.features.compromiso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.features.compromiso.service.CompromisoInternoService;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Rutas para soporte-service; exigen X-Internal-Api-Key
@RestController
@Tag(name = "Interno: compromisos para soporte")
@SecurityRequirement(name = "internalApiKey")
@RequestMapping("/api/v1/internal/compromisos")
@RequiredArgsConstructor
public class CompromisoInternoController {
    private final CompromisoInternoService service;

    @Operation(summary = "Saber si un compromiso se puede reportar y de quién es")
    @GetMapping("/{folio}/reportabilidad")
    public CompromisoInterno reportabilidad(@PathVariable String folio,
                                            @RequestParam(required = false) UUID usuarioId) {
        return service.reportabilidad(folio, usuarioId);
    }

    @Operation(summary = "Poner el compromiso en revisión")
    @PutMapping("/{folio}/revision")
    public CompromisoInterno revision(@PathVariable String folio) {
        return service.marcarEnRevision(folio);
    }

    @Operation(summary = "Sacar el compromiso de revisión")
    @PutMapping("/{folio}/fin-revision")
    public CompromisoInterno finRevision(@PathVariable String folio) {
        return service.salirDeRevision(folio);
    }
}
