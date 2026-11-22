package com.pagoflex.mensajeria.features.conversacion.dto.response;

import java.util.List;

/// mensajes del más antiguo al más nuevo; con el cursor se piden los anteriores
public record PaginaMensajesResponse(
        List<MensajeResponse> mensajes,
        Long cursorAnterior
) {
}
