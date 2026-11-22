package com.pagoflex.cobranza.features.empresa.controller;

import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Empresa de cada ejecutivo para otros servicios; exigen X-Internal-Api-Key
@RestController
@RequestMapping("/api/v1/internal/empresas/ejecutivos")
@RequiredArgsConstructor
public class EjecutivoInternoController {
    private final EjecutivoService service;

    @GetMapping("/{usuarioId}")
    public EmpresaInterna empresa(@PathVariable UUID usuarioId) {
        return service.interna(usuarioId);
    }
}
