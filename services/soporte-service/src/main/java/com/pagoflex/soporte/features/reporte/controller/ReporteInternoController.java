package com.pagoflex.soporte.features.reporte.controller;

import com.pagoflex.contracts.soporte.ParticipantesReporteInterno;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Rutas para otros servicios, protegidas con la clave interna
@RestController
@RequestMapping("/api/v1/internal/reportes")
@RequiredArgsConstructor
public class ReporteInternoController {
    private final ReporteService service;

    @GetMapping("/{numero}/participantes")
    public ResponseEntity<ParticipantesReporteInterno> participantes(@PathVariable String numero) {
        return ResponseEntity.ok(service.participantes(numero));
    }
}
