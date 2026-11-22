package com.pagoflex.reconocimientos.features.recognition.domain;

import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;

public record RecognitionCandidate(RecognitionCode code, String reference, String reason) {
    public static final String NO_REFERENCE = "-";
}
