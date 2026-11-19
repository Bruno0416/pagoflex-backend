package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.features.empresa.entity.EjecutivoEmpresa;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EjecutivoSinEmpresa;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EjecutivoEmpresaRepository;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.core.error.PagoFlexException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EjecutivoServiceImpl implements EjecutivoService {
    private final EjecutivoEmpresaRepository ejecutivoRepo;
    private final EmpresaClienteRepository empresaRepo;

    // el ejecutivo solo trabaja con su propia empresa RN-22
    @Override
    @Transactional(readOnly = true)
    public EmpresaCliente empresaDe(UUID usuarioId) {
        if (usuarioId == null) throw new EjecutivoSinEmpresa();
        return ejecutivoRepo.findConEmpresa(usuarioId)
                .map(EjecutivoEmpresa::getEmpresa)
                .orElseThrow(EjecutivoSinEmpresa::new);
    }

    @Override
    @Transactional(readOnly = true)
    public EmpresaInterna interna(UUID usuarioId) {
        // para otros servicios un ejecutivo sin empresa es un recurso inexistente
        EmpresaCliente empresa = ejecutivoRepo.findConEmpresa(usuarioId)
                .map(EjecutivoEmpresa::getEmpresa)
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
}
