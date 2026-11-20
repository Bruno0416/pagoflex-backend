package com.pagoflex.reconocimientos.features.recognition.dto.response;

import java.time.OffsetDateTime;

public record UserRecognitionResponse(String codigo, String nombre, String descripcion, String motivo,
                                      OffsetDateTime fechaObtencion) {}
