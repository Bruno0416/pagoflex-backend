package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.organization.AuthorizedRole;
import com.pagoflex.contracts.organization.OrganizationType;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeResponse;
import com.pagoflex.core.error.PagoFlexException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CodigoEmpresaService {
    private final EmpresaClienteRepository empresaRepo;
    private final PasswordEncoder encoder;

    // el código compartido de la organización define el rol que puede registrarse
    @Transactional(readOnly = true)
    public ValidateOrganizationCodeResponse validar(String codigo) {
        String limpio = codigo.trim().toUpperCase(Locale.ROOT);

        // 1. comparar contra el hash de cada organización; el código no se guarda ni se registra
        EmpresaCliente empresa = empresaRepo.findAll().stream()
                .filter(e -> e.getCodigoRegistroHash() != null && e.getTipo() != null)
                .filter(e -> encoder.matches(limpio, e.getCodigoRegistroHash()))
                .findFirst()
                .orElseThrow(() -> new PagoFlexException(404, "El código de empresa no es válido"));

        // 2. PagoFlex registra agentes; las empresas cliente, ejecutivos
        AuthorizedRole rol = empresa.getTipo() == OrganizationType.INTERNA ? AuthorizedRole.AGENTE : AuthorizedRole.EJECUTIVO;
        return new ValidateOrganizationCodeResponse(empresa.getId(), empresa.getNombreFantasia(), empresa.getTipo(), rol);
    }
}
