package com.pagoflex.cobranza.features.compromiso.dto.request;

import com.pagoflex.cobranza.features.compromiso.entity.MotivoAnulacion;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AnularCompromisoRequest(
        @NotNull(message = "Elige el motivo de la anulación")
        MotivoAnulacion motivo,

        @Size(max = 500, message = "El comentario no puede superar los 500 caracteres")
        String comentario
) {
}
