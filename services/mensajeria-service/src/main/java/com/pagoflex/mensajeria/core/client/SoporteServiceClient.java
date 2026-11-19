package com.pagoflex.mensajeria.core.client;

import com.pagoflex.mensajeria.core.exceptions.ServicioNoDisponible;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/// El estado del reporte y su agente los sabe soporte-service
@Component
public class SoporteServiceClient {
    private final WebClient client;

    /// lo que soporte comparte para decidir quién conversa
    public record Participantes(String numero, String folio, String estado, UUID empresaId, UUID agenteId,
                                UUID usuarioId) {
    }

    public SoporteServiceClient(
            WebClient.Builder builder,
            @Value("${soporte-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. cliente con la clave interna en cada llamada
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    public Optional<Participantes> participantes(String numero) {
        try {
            return Optional.ofNullable(client.get().uri("/reportes/{numero}/participantes", numero)
                    .retrieve().bodyToMono(Participantes.class).block(Duration.ofSeconds(8)));
        } catch (WebClientResponseException.NotFound ex) {
            // 1. el reporte no existe
            return Optional.empty();
        } catch (WebClientException ex) {
            // 2. soporte caído o lento
            throw new ServicioNoDisponible();
        }
    }
}
