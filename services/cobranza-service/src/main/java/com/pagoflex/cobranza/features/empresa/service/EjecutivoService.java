package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.contracts.cobranza.EmpresaInterna;

import java.util.UUID;

public interface EjecutivoService {
    EmpresaCliente empresaDe(UUID usuarioId);

    EmpresaInterna interna(UUID usuarioId);

    void asignar(UUID usuarioId, String codigoEmpresa);
}
