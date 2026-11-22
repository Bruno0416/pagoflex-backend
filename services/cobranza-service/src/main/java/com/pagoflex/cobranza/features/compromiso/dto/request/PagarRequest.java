package com.pagoflex.cobranza.features.compromiso.dto.request;

import com.pagoflex.cobranza.features.compromiso.entity.MedioPago;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/// el total no viaja: se confirma la cotización que el servidor calculó
public record PagarRequest(
        @NotNull(message = "Elige el medio de pago")
        MedioPago medioPago,

        @NotBlank(message = "Falta la cotización del pago")
        String cotizacionId
) {
}
