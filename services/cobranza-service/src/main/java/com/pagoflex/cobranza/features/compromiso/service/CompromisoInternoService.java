package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.contracts.cobranza.CompromisoInterno;

/// Operaciones que otros servicios piden por la red interna, por ejemplo soporte-service
public interface CompromisoInternoService {
    CompromisoInterno obtener(String folio);

    CompromisoInterno marcarEnRevision(String folio);

    CompromisoInterno salirDeRevision(String folio);
}
