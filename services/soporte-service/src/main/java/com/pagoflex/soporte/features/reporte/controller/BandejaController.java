package com.pagoflex.soporte.features.reporte.controller;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.service.ReporteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/// Bandeja del agente de soporte RN-23
@RestController
@RequestMapping("/api/v1/soporte/bandeja")
@RequiredArgsConstructor
public class BandejaController {
    private final ReporteService service;

    @GetMapping
    public ResponseEntity<List<ReporteResponse>> bandeja(@RequestParam(required = false) EstadoReporte estado) {
        return ResponseEntity.ok(service.bandeja(estado));
    }

    @GetMapping("/{numero}")
    public ResponseEntity<ReporteResponse> detalle(@PathVariable String numero) {
        return ResponseEntity.ok(service.detalle(numero));
    }

    @PostMapping("/{numero}/tomar")
    public ResponseEntity<ReporteResponse> tomar(@PathVariable String numero) {
        return ResponseEntity.ok(service.tomar(CurrentUser.get().email(), numero));
    }

    @PostMapping("/{numero}/resolver")
    public ResponseEntity<ReporteResponse> resolver(@PathVariable String numero,
                                                    @Valid @RequestBody ResolverRequest request) {
        return ResponseEntity.ok(service.resolver(CurrentUser.get().email(), numero, request));
    }
}
