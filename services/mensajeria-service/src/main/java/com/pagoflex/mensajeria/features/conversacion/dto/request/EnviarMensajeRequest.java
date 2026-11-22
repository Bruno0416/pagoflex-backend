package com.pagoflex.mensajeria.features.conversacion.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnviarMensajeRequest(
        @NotBlank(message = "Escribe un mensaje")
        @Size(max = 1000, message = "El mensaje no puede superar los 1000 caracteres")
        String texto,

        @NotBlank(message = "Falta el identificador del mensaje")
        @Size(max = 64, message = "El identificador no puede superar los 64 caracteres")
        String clientMessageId
) {
}
