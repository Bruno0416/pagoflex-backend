package com.pagoflex.cobranza.features.empresa.controller;

import com.pagoflex.cobranza.features.empresa.service.CodigoEmpresaService;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeRequest;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/// Usuarios-service valida aquí el código de empresa antes de crear un agente o ejecutivo
@RestController
@RequestMapping("/api/v1/internal/empresas/codigos")
@RequiredArgsConstructor
public class CodigoEmpresaInternoController {
    private final CodigoEmpresaService service;

    @PostMapping("/validar")
    public ValidateOrganizationCodeResponse validar(@Valid @RequestBody ValidateOrganizationCodeRequest request) {
        return service.validar(request.codigo());
    }
}
