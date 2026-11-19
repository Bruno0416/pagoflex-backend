package com.pagoflex.soporte.features.reporte.dto.response;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;

import java.util.UUID;

/// Quiénes pueden conversar sobre un reporte; lo usará mensajería
public record ParticipantesResponse(
        String numero,
        String folio,
        EstadoReporte estado,
        UUID empresaId,
        UUID agenteId,
        UUID usuarioId
) {
}
