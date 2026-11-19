package com.pagoflex.mensajeria.features.conversacion.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record LecturaRequest(
        @NotNull @PositiveOrZero Long ultimaSecuencia
) {
}
