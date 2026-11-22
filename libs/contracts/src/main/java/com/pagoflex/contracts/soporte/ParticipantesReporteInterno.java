package com.pagoflex.contracts.soporte;

import java.util.UUID;

/**
 * Participantes autorizados de un reporte.
 * El estado viaja como texto para no compartir el enum JPA de Soporte.
 */
public record ParticipantesReporteInterno(
        String numero,
        String folio,
        String estado,
        UUID empresaId,
        UUID agenteId,
        UUID usuarioId
) {}
