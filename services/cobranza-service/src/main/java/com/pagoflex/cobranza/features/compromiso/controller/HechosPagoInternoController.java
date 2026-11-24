package com.pagoflex.cobranza.features.compromiso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.features.compromiso.service.HechosPagoService;
import com.pagoflex.contracts.cobranza.RespuestaHechosPago;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/// Hechos de pago para reconocimientos-service; exigen X-Internal-Api-Key
@RestController
@Tag(name = "Interno: hechos de pago para reconocimientos")
@SecurityRequirement(name = "internalApiKey")
@RequestMapping("/api/v1/internal/usuarios")
@RequiredArgsConstructor
public class HechosPagoInternoController {
    private final HechosPagoService service;

    @Operation(summary = "Compromisos y pagos de un usuario")
    @GetMapping("/{usuarioId}/hechos-pago")
    public ResponseEntity<RespuestaHechosPago> hechosPago(@PathVariable UUID usuarioId) {
        return ResponseEntity.ok(service.hechos(usuarioId));
    }
}
