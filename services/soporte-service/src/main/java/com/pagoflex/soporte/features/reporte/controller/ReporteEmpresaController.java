package com.pagoflex.soporte.features.reporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.soporte.features.reporte.dto.response.PaginaResponse;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/// Reportes de los cobros de la empresa del ejecutivo
@RestController
@Tag(name = "Ejecutivo: reportes de su empresa")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/empresas/me")
@RequiredArgsConstructor
public class ReporteEmpresaController {
    private final ReporteService service;

    @Operation(summary = "Reportes de los compromisos de su empresa (paginado)")
    @GetMapping("/reportes")
    public ResponseEntity<PaginaResponse<ReporteResponse>> reportes(
            @RequestParam(required = false) EstadoReporte estado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.deMiEmpresa(CurrentUser.id(), estado, page, size));
    }

    @Operation(summary = "Reporte vigente de un compromiso")
    @GetMapping("/compromisos/{folio}/reporte-activo")
    public ResponseEntity<ReporteResponse> reporteActivo(@PathVariable String folio) {
        return ResponseEntity.ok(service.reporteActivo(CurrentUser.id(), folio));
    }
}
