package com.pagoflex.cobranza.features.empresa.controller;

import com.pagoflex.cobranza.features.empresa.dto.request.AsignarEmpresaRequest;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Uso interno: asocia un ejecutivo de usuarios-service a su empresa
@RestController
@RequestMapping("/api/v1/internal/cobranza/ejecutivos")
@RequiredArgsConstructor
public class EjecutivoInternoController {
    private final EjecutivoService service;

    @PutMapping
    public ResponseEntity<Void> asignar(@Valid @RequestBody AsignarEmpresaRequest request) {
        service.asignar(request.correo(), request.codigoEmpresa());
        return ResponseEntity.noContent().build();
    }
}
