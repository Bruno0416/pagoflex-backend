package com.pagoflex.reconocimientos.features.recognition.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record UserRecognitionResponse(
        @Schema(description = "Código del reconocimiento obtenido", example = "PRIMER_PAGO_A_TIEMPO")
        String codigo,
        @Schema(description = "Nombre del reconocimiento", example = "Primer pago a tiempo")
        String nombre,
        @Schema(description = "Descripción del logro", example = "Tu primer compromiso pagado a tiempo")
        String descripcion,
        @Schema(description = "Hecho de pago que originó el reconocimiento", example = "Pago CP-2026-0105 del 2026-10-06")
        String motivo,
        @Schema(description = "Fecha y hora de obtención con zona horaria", example = "2026-10-06T10:30:00-03:00")
        OffsetDateTime fechaObtencion
) {}
