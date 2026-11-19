package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.contracts.cobranza.CompromisoInterno;

import java.util.UUID;

public interface CompromisoInternoService {
    CompromisoInterno reportabilidad(String folio, UUID usuarioId);

    CompromisoInterno marcarEnRevision(String folio);

    CompromisoInterno salirDeRevision(String folio);
}
