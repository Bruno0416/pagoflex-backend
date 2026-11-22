package com.pagoflex.cobranza.features.compromiso.controller;

import com.pagoflex.cobranza.features.compromiso.service.CompromisoInternoService;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Rutas para soporte-service; exigen X-Internal-Api-Key
@RestController
@RequestMapping("/api/v1/internal/compromisos")
@RequiredArgsConstructor
public class CompromisoInternoController {
    private final CompromisoInternoService service;

    @GetMapping("/{folio}/reportabilidad")
    public CompromisoInterno reportabilidad(@PathVariable String folio,
                                            @RequestParam(required = false) UUID usuarioId) {
        return service.reportabilidad(folio, usuarioId);
    }

    @PutMapping("/{folio}/revision")
    public CompromisoInterno revision(@PathVariable String folio) {
        return service.marcarEnRevision(folio);
    }

    @PutMapping("/{folio}/fin-revision")
    public CompromisoInterno finRevision(@PathVariable String folio) {
        return service.salirDeRevision(folio);
    }
}
