package com.pagoflex.mensajeria.features.conversacion.service;

import com.pagoflex.mensajeria.features.conversacion.dto.response.MensajeResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/// Conexiones sse abiertas por reporte; cada mensaje nuevo se manda a todas
@Slf4j
@Component
public class EventosConversacion {
    private static final long DURACION_MS = 30 * 60 * 1000L;

    private final Map<String, List<SseEmitter>> abiertas = new ConcurrentHashMap<>();

    public SseEmitter abrir(String numero, List<MensajeResponse> pendientes) {
        // 1. conexión que se cierra sola a los 30 minutos; la app vuelve a conectar con Last-Event-ID
        SseEmitter emitter = new SseEmitter(DURACION_MS);
        List<SseEmitter> lista = abiertas.computeIfAbsent(numero, k -> new CopyOnWriteArrayList<>());
        lista.add(emitter);
        emitter.onCompletion(() -> lista.remove(emitter));
        emitter.onTimeout(() -> lista.remove(emitter));
        emitter.onError(e -> lista.remove(emitter));

        // 2. lo que se perdió mientras estuvo desconectada, en orden
        try {
            emitter.send(SseEmitter.event().name("ready").data("ok"));
            for (MensajeResponse m : pendientes) enviar(emitter, m);
        } catch (IOException | IllegalStateException ex) {
            lista.remove(emitter);
        }
        return emitter;
    }

    public void publicar(String numero, MensajeResponse mensaje) {
        for (SseEmitter emitter : abiertas.getOrDefault(numero, List.of())) {
            try {
                enviar(emitter, mensaje);
            } catch (IOException | IllegalStateException ex) {
                // 1. la app se fue: se descarta esa conexión
                abiertas.get(numero).remove(emitter);
            }
        }
    }

    public int conexiones(String numero) {
        return abiertas.getOrDefault(numero, List.of()).size();
    }

    // --- helper privado ---
    private static void enviar(SseEmitter emitter, MensajeResponse m) throws IOException {
        emitter.send(SseEmitter.event()
                .id(String.valueOf(m.secuencia()))
                .name("message")
                .data(m, MediaType.APPLICATION_JSON));
    }
}
