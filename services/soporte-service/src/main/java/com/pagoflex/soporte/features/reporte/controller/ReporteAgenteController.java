package com.pagoflex.soporte.features.reporte.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Agente: bandeja de reportes")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/reportes")
@RequiredArgsConstructor
public class ReporteAgenteController {
    private final ReporteService service;

    @Operation(summary = "Bandeja del agente: abiertos y asignados a él (paginado)")
    @GetMapping
    public ResponseEntity<PaginaResponse<ReporteResponse>> bandeja(
            @RequestParam(required = false) EstadoReporte estado,
            @RequestParam(required = false) Boolean atrasado,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.bandeja(CurrentUser.id(), estado, atrasado, page, size));
    }

    @Operation(summary = "Detalle de un reporte")
    @GetMapping("/{numero}")
    public ResponseEntity<ReporteResponse> detalle(@PathVariable String numero) {
        return ResponseEntity.ok(service.detalle(CurrentUser.id(), numero));
    }

    @Operation(summary = "Tomar el reporte (ABIERTO → ASIGNADO)")
    @PostMapping("/{numero}/asignaciones")
    public ResponseEntity<ReporteResponse> asignar(@PathVariable String numero) {
        return ResponseEntity.ok(service.asignar(CurrentUser.get(), numero));
    }

    @Operation(summary = "Iniciar la atención (ASIGNADO → EN_ATENCION)")
    @PostMapping("/{numero}/atenciones")
    public ResponseEntity<ReporteResponse> atender(@PathVariable String numero) {
        return ResponseEntity.ok(service.atender(CurrentUser.get(), numero));
    }

    @Operation(summary = "Resolver con respuesta (EN_ATENCION → RESUELTO)")
    @PostMapping("/{numero}/resoluciones")
    public ResponseEntity<ReporteResponse> resolver(@PathVariable String numero,
                                                    @Valid @RequestBody ResolverRequest request) {
        return ResponseEntity.ok(service.resolver(CurrentUser.get(), numero, request));
    }
}
