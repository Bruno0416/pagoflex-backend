package com.pagoflex.soporte.features.reporte.dto.request;

import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportarRequest(
        @NotBlank(message = "El folio es obligatorio")
        String folio,

        @NotNull(message = "Elige un motivo")
        MotivoReporte motivo,

        @Size(max = 500, message = "El comentario no puede superar 500 caracteres")
        String comentario
) {
}
