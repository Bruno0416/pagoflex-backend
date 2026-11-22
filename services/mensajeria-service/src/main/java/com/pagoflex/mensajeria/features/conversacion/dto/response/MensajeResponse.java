package com.pagoflex.mensajeria.features.conversacion.dto.response;

import com.pagoflex.mensajeria.features.conversacion.entity.RolParticipante;

import java.time.LocalDateTime;
import java.util.UUID;

/// mismo formato por rest y por el evento sse "message"
public record MensajeResponse(
        UUID id,
        String reporteNumero,
        UUID autorId,
        String autor,
        RolParticipante rol,
        String texto,
        LocalDateTime enviadoEn,
        String clientMessageId,
        long secuencia
) {
}
