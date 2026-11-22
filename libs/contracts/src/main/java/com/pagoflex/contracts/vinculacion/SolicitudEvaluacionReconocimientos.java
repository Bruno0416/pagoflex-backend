package com.pagoflex.contracts.vinculacion;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record SolicitudEvaluacionReconocimientos(@NotNull UUID idUsuario, UUID idEvento) {}
