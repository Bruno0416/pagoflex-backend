package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.features.empresa.entity.EjecutivoEmpresa;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EjecutivoSinEmpresa;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EjecutivoEmpresaRepository;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import com.pagoflex.core.utils.EmailUtils;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class EjecutivoServiceImpl implements EjecutivoService {
    private final EjecutivoEmpresaRepository ejecutivoRepo;
    private final EmpresaClienteRepository empresaRepo;

    // el ejecutivo solo trabaja con su propia empresa RN-22
    @Override
    public EmpresaCliente empresaDe(String correo) {
        if (correo == null) throw new EjecutivoSinEmpresa();
        return ejecutivoRepo.findConEmpresa(EmailUtils.normalize(correo))
                .map(EjecutivoEmpresa::getEmpresa)
                .orElseThrow(EjecutivoSinEmpresa::new);
    }

    @Override
    @Transactional
    public void asignar(String correo, String codigoEmpresa) {
        // 1. buscar la empresa por su código
        EmpresaCliente empresa = empresaRepo.findByCodigo(codigoEmpresa.trim().toUpperCase())
                .orElseThrow(() -> new EmpresaNoEncontrada(codigoEmpresa));

        // 2. crear o reemplazar la asignación del ejecutivo
        ejecutivoRepo.save(EjecutivoEmpresa.builder()
                .correo(EmailUtils.normalize(correo))
                .empresa(empresa)
                .build());
    }
}
