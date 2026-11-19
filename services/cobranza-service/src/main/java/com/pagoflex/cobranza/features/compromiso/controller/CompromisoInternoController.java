package com.pagoflex.cobranza.features.compromiso.controller;

import com.pagoflex.cobranza.features.compromiso.service.CompromisoInternoService;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Uso interno con X-Internal-Api-Key; el gateway no expone estas rutas
@RestController
@RequestMapping("/api/v1/internal/cobranza/compromisos")
@RequiredArgsConstructor
public class CompromisoInternoController {
    private final CompromisoInternoService service;

    @GetMapping("/{folio}")
    public ResponseEntity<CompromisoInterno> obtener(@PathVariable String folio) {
        return ResponseEntity.ok(service.obtener(folio));
    }

    @PostMapping("/{folio}/revision")
    public ResponseEntity<CompromisoInterno> marcarEnRevision(@PathVariable String folio) {
        return ResponseEntity.ok(service.marcarEnRevision(folio));
    }

    @DeleteMapping("/{folio}/revision")
    public ResponseEntity<CompromisoInterno> salirDeRevision(@PathVariable String folio) {
        return ResponseEntity.ok(service.salirDeRevision(folio));
    }
}
