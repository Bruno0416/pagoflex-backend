package com.pagoflex.mensajeria.features.conversacion.dto.response;

import java.time.LocalDateTime;

public record ConversacionResponse(
        String reporteNumero,
        String estadoReporte,
        boolean soloLectura,
        long ultimaSecuencia,
        long ultimaSecuenciaLeida,
        LocalDateTime fechaCreacion
) {
}
