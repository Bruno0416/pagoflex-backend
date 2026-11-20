package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.core.client.PersonaInterna;
import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.empresa.entity.EjecutivoEmpresa;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EjecutivoSinEmpresa;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EjecutivoEmpresaRepository;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.contracts.organization.OrganizationType;
import com.pagoflex.core.error.PagoFlexException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EjecutivoServiceImpl implements EjecutivoService {
    private final EjecutivoEmpresaRepository ejecutivoRepo;
    private final EmpresaClienteRepository empresaRepo;
    private final UsuariosServiceClient usuarios;

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

    @Override
    @Transactional
    public void asignar(UUID usuarioId, String codigoEmpresa) {
        // 1. buscar la empresa por su código
        EmpresaCliente empresa = empresaRepo.findByCodigo(codigoEmpresa.trim().toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new EmpresaNoEncontrada(codigoEmpresa));

        // 2. crear o reemplazar la asignación del ejecutivo
        ejecutivoRepo.save(EjecutivoEmpresa.builder()
                .usuarioId(usuarioId)
                .empresa(empresa)
                .build());
    }

    // --- helper privado ---
    private Optional<EmpresaCliente> buscar(UUID usuarioId) {
        // 1. asignación hecha aquí (cuentas de ejemplo)
        Optional<EmpresaCliente> local = ejecutivoRepo.findConEmpresa(usuarioId).map(EjecutivoEmpresa::getEmpresa);
        if (local.isPresent()) return local;

        // 2. empresa que quedó en su persona al registrarse con el código de empresa
        return usuarios.porUsuario(usuarioId)
                .map(PersonaInterna::empresaId)
                .flatMap(empresaRepo::findById)
                .filter(e -> e.getTipo() == OrganizationType.CLIENTE);
    }
}
