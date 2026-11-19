package com.pagoflex.soporte.features.reporte.controller;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/// Reportes del usuario final sobre sus cobros
@RestController
@RequestMapping("/api/v1/soporte/reportes")
@RequiredArgsConstructor
public class ReporteController {
    private final ReporteService service;

    @PostMapping
    public ResponseEntity<ReporteResponse> reportar(@Valid @RequestBody ReportarRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.reportar(CurrentUser.get(), request));
    }

    @GetMapping("/mios")
    public ResponseEntity<List<ReporteResponse>> misReportes() {
        return ResponseEntity.ok(service.misReportes(CurrentUser.get().email()));
    }
}
