package com.pagoflex.soporte.features.reporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.response.PaginaResponse;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Reportes del usuario final sobre sus cobros
@RestController
@Tag(name = "Usuario final: reportes")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteUsuarioController {
    private final ReporteService service;

    @Operation(summary = "Reportar un problema con un compromiso")
    @PostMapping
    public ResponseEntity<ReporteResponse> reportar(@Valid @RequestBody ReportarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reportar(CurrentUser.get(), request));
    }

    @Operation(summary = "Mis reportes (paginado)")
    @GetMapping("/me")
    public ResponseEntity<PaginaResponse<ReporteResponse>> misReportes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.misReportes(CurrentUser.id(), page, size));
    }

    @Operation(summary = "Detalle de un reporte propio")
    @GetMapping("/me/{numero}")
    public ResponseEntity<ReporteResponse> miReporte(@PathVariable String numero) {
        return ResponseEntity.ok(service.miReporte(CurrentUser.id(), numero));
    }
}
