package com.pagoflex.cobranza.core.client;

import com.pagoflex.contracts.vinculacion.SolicitudEvaluacionReconocimientos;
import com.pagoflex.security.InternalApiKeyFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.UUID;

/// Pide a reconocimientos-service que evalúe a un usuario después de confirmar un pago.
/// Es una sola llamada HTTP sin reintentos ni cola: si falla, el pago ya confirmado se mantiene y el reconocimiento
/// se otorga en la siguiente evaluación (otro pago, repetir el mismo pago con su Idempotency-Key o GET /reconocimientos/me).
@Slf4j
@Component
public class ReconocimientosClient {
    private final RestClient client;

    public ReconocimientosClient(
            @Value("${reconocimientos-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. clave interna en cada llamada y espera corta para no retrasar la respuesta del pago
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalKey).build();
    }

    /// true si reconocimientos aceptó la evaluación; nunca lanza excepciones
    public boolean evaluar(UUID usuarioId, UUID eventoId) {
        try {
            client.post().uri("/evaluar").body(new SolicitudEvaluacionReconocimientos(usuarioId, eventoId))
                    .retrieve().toBodilessEntity();
            return true;
        } catch (RestClientException ex) {
            log.warn("no se pudo evaluar reconocimientos tras el pago: {}", ex.getClass().getSimpleName());
            return false;
        }
    }
}
