package com.pagoflex.contracts.cobranza;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;


public record PaymentFact(@NotBlank String numeroComprobante, @NotNull OffsetDateTime fechaHoraPago) {}
