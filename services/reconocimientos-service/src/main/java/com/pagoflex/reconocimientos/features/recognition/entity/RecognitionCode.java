package com.pagoflex.reconocimientos.features.recognition.entity;


public enum RecognitionCode {
    PRIMER_PAGO_A_TIEMPO("Primer pago a tiempo", "Tu primer compromiso pagado a tiempo"),
    TRES_MESES_AL_DIA("Tres meses al día", "Tres meses seguidos con tus pagos al día");

    private final String displayName;
    private final String description;

    RecognitionCode(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }
}
