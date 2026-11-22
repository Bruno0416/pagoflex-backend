package com.pagoflex.mensajeria.features.conversacion.service;

import com.pagoflex.mensajeria.features.conversacion.dto.request.EnviarMensajeRequest;
import com.pagoflex.mensajeria.features.conversacion.dto.response.ConversacionResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.MensajeResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.PaginaMensajesResponse;
import com.pagoflex.security.AuthenticatedUser;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ConversacionService {
    ConversacionResponse abrir(AuthenticatedUser usuario, String numero);

    PaginaMensajesResponse mensajes(AuthenticatedUser usuario, String numero, Long cursor, Integer limite);

    Enviado enviar(AuthenticatedUser usuario, String numero, EnviarMensajeRequest request);

    SseEmitter eventos(AuthenticatedUser usuario, String numero, Long ultimoVisto);

    void marcarLeido(AuthenticatedUser usuario, String numero, long ultimaSecuencia);

    /// nuevo es false cuando la app reintentó el mismo clientMessageId
    record Enviado(MensajeResponse mensaje, boolean nuevo) {
    }
}
