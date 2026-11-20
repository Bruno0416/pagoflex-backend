package com.pagoflex.cobranza.features.codigo.controller;

import com.pagoflex.cobranza.features.codigo.service.CodigoRegistroService;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeRequest;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/// Validación del código de registro para usuarios-service; exige X-Internal-Api-Key
@RestController
@RequestMapping("/api/v1/internal/empresas/codigos")
@RequiredArgsConstructor
public class CodigoRegistroInternoController {
    private final CodigoRegistroService service;

    @PostMapping("/validar")
    public ResponseEntity<ValidateOrganizationCodeResponse> validar(
            @Valid @RequestBody ValidateOrganizationCodeRequest request) {
        return ResponseEntity.ok(service.validar(request.codigo()));
    }
}
