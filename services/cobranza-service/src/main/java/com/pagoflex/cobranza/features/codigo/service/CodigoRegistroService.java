package com.pagoflex.cobranza.features.codigo.service;

import com.pagoflex.contracts.organizacion.RespuestaValidacionCodigoEmpresa;

import java.time.Instant;
import java.util.UUID;

public interface CodigoRegistroService {
    /// Comprueba un código de registro; cualquier fallo responde igual (CodigoInvalido)
    RespuestaValidacionCodigoEmpresa validar(String codigo);

    /// Genera un código de agente de la organización interna; el valor solo se devuelve aquí, nunca se guarda
    String generarParaAgente(Instant expiraEn);

    /// Genera un código de ejecutivo para una empresa cliente existente
    String generarParaEjecutivo(UUID empresaId, Instant expiraEn);

    /// Revoca el código con ese prefijo
    void revocar(String prefijo);
}
