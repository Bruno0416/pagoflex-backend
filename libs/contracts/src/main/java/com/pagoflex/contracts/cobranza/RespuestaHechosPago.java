package com.pagoflex.contracts.cobranza;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


public record RespuestaHechosPago(@NotNull UUID idUsuario, @NotNull OffsetDateTime generadoEn,
                                   @NotNull List<@NotNull @Valid HechoCompromiso> compromisos) {}
