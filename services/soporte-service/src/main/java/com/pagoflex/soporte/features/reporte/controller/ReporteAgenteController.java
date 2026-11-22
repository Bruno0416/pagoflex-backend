package com.pagoflex.soporte.features.reporte.controller;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.PaginaResponse;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Bandeja y gestión del agente de soporte RN-23
@RestController
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteAgenteController {
    private final ReporteService service;

    @GetMapping
    public ResponseEntity<PaginaResponse<ReporteResponse>> bandeja(
            @RequestParam(required = false) EstadoReporte estado,
            @RequestParam(required = false) Boolean atrasado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.bandeja(CurrentUser.id(), estado, atrasado, page, size));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<ReporteResponse> detalle(@PathVariable String numero) {
        return ResponseEntity.ok(service.detalle(CurrentUser.id(), numero));
    }

    @PostMapping("/{numero}/asignaciones")
    public ResponseEntity<ReporteResponse> asignar(@PathVariable String numero) {
        return ResponseEntity.ok(service.asignar(CurrentUser.get(), numero));
    }

    @PostMapping("/{numero}/atenciones")
    public ResponseEntity<ReporteResponse> atender(@PathVariable String numero) {
        return ResponseEntity.ok(service.atender(CurrentUser.get(), numero));
    }

    @PostMapping("/{numero}/resoluciones")
    public ResponseEntity<ReporteResponse> resolver(@PathVariable String numero,
                                                    @Valid @RequestBody ResolverRequest request) {
        return ResponseEntity.ok(service.resolver(CurrentUser.get(), numero, request));
    }
}
