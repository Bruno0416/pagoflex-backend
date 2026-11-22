package com.pagoflex.contracts.vinculacion;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RespuestaEvaluacionReconocimientos(UUID idUsuario, List<String> otorgados, OffsetDateTime evaluadoEn) {}
