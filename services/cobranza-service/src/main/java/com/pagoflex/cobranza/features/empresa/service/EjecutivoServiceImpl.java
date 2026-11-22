package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EjecutivoSinEmpresa;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.core.error.PagoFlexException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/// La empresa del ejecutivo es la de su persona en usuarios-service, fijada al registrarse con un código válido;
/// cobranza no guarda otra copia de esa asociación.
@Service
@RequiredArgsConstructor
public class EjecutivoServiceImpl implements EjecutivoService {
    private final UsuariosServiceClient usuarios;
    private final EmpresaClienteRepository empresaRepo;

    // el ejecutivo solo trabaja con su propia empresa RN-22
    @Override
    @Transactional(readOnly = true)
    public EmpresaCliente empresaDe(UUID usuarioId) {
        if (usuarioId == null) throw new EjecutivoSinEmpresa();
        return buscar(usuarioId).orElseThrow(EjecutivoSinEmpresa::new);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaInterna interna(UUID usuarioId) {
        // para otros servicios un ejecutivo sin empresa es un recurso inexistente
        EmpresaCliente empresa = buscar(usuarioId)
                .orElseThrow(() -> new PagoFlexException(404, "El ejecutivo no tiene una empresa asociada"));
        return new EmpresaInterna(empresa.getId(), empresa.getCodigo(), empresa.getNombreFantasia());
    }

    // --- helper privado ---
    private Optional<EmpresaCliente> buscar(UUID usuarioId) {
        // 1. la persona de la cuenta dice a qué empresa cliente pertenece; agentes y usuarios finales no tienen
        return usuarios.porUsuario(usuarioId)
                .map(persona -> persona.empresaId())
                .flatMap(empresaRepo::findById);
    }
}
