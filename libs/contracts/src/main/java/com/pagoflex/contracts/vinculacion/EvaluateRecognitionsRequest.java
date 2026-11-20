package com.pagoflex.contracts.vinculacion;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;


public record EvaluateRecognitionsRequest(@NotNull UUID idUsuario, UUID idEvento) {}
