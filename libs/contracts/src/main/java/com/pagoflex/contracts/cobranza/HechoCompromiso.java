package com.pagoflex.contracts.cobranza;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record HechoCompromiso(@NotNull UUID idCompromiso, @NotBlank String folio,
                             @NotNull LocalDate fechaVencimiento, @NotNull EstadoCompromisoEvaluacion estado,
                             @Valid HechoPago pago) {}
