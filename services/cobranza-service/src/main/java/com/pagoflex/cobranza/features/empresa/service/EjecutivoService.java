package com.pagoflex.cobranza.features.empresa.service;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;


public interface EjecutivoService {
    EmpresaCliente empresaDe(String correo);

    void asignar(String correo, String codigoEmpresa);
}
