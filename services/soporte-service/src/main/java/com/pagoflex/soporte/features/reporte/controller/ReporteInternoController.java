package com.pagoflex.soporte.features.reporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.contracts.soporte.ParticipantesReporteInterno;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Rutas para otros servicios, protegidas con la clave interna
@RestController
@Tag(name = "Interno: participantes del reporte")
@SecurityRequirement(name = "internalApiKey")
@RequestMapping("/api/v1/internal/reportes")
@RequiredArgsConstructor
public class ReporteInternoController {
    private final ReporteService service;

    @Operation(summary = "Agente, empresa y estado de un reporte (para mensajería)")
    @GetMapping("/{numero}/participantes")
    public ResponseEntity<ParticipantesReporteInterno> participantes(@PathVariable String numero) {
        return ResponseEntity.ok(service.participantes(numero));
    }
}
