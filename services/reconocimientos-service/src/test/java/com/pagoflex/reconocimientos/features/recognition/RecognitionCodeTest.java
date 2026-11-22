package com.pagoflex.reconocimientos.features.recognition;

import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecognitionCodeTest {
    // el catalogo del MVP contiene unicamente los dos reconocimientos funcionales
    @Test
    void catalogContainsOnlyTheTwoFunctionalRecognitions() {
        assertThat(RecognitionCode.values())
                .containsExactly(RecognitionCode.PRIMER_PAGO_A_TIEMPO, RecognitionCode.TRES_MESES_AL_DIA);
    }

    @Test
    void everyCodeHasDisplayNameAndDescription() {
        for (RecognitionCode code : RecognitionCode.values()) {
            assertThat(code.displayName()).isNotBlank();
            assertThat(code.description()).isNotBlank();
        }
    }
}
