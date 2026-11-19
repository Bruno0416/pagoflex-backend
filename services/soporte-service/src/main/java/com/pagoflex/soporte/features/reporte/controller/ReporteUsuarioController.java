package com.pagoflex.soporte.features.reporte.controller;

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
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteUsuarioController {
    private final ReporteService service;

    @PostMapping
    public ResponseEntity<ReporteResponse> reportar(@Valid @RequestBody ReportarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reportar(CurrentUser.get(), request));
    }

    @GetMapping("/me")
    public ResponseEntity<PaginaResponse<ReporteResponse>> misReportes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.misReportes(CurrentUser.id(), page, size));
    }

    @GetMapping("/me/{numero}")
    public ResponseEntity<ReporteResponse> miReporte(@PathVariable String numero) {
        return ResponseEntity.ok(service.miReporte(CurrentUser.id(), numero));
    }
}
