package com.pagoflex.cobranza.features.empresa.controller;

import com.pagoflex.cobranza.features.empresa.dto.request.AsignarEmpresaRequest;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    // mientras no exista el registro con código de empresa
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignar(@Valid @RequestBody AsignarEmpresaRequest request) {
        service.asignar(request.usuarioId(), request.codigoEmpresa());
    }
}
