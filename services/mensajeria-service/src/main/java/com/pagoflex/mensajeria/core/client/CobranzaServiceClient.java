package com.pagoflex.mensajeria.core.client;

import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.mensajeria.core.exceptions.ServicioNoDisponible;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/// La empresa de cada ejecutivo la sabe cobranza-service
@Component
public class CobranzaServiceClient {
    private final WebClient client;

    public CobranzaServiceClient(
            WebClient.Builder builder,
            @Value("${cobranza-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    public Optional<EmpresaInterna> empresaDeEjecutivo(UUID usuarioId) {
        try {
            return Optional.ofNullable(client.get().uri("/empresas/ejecutivos/{id}", usuarioId)
                    .retrieve().bodyToMono(EmpresaInterna.class).block(Duration.ofSeconds(8)));
        } catch (WebClientResponseException.NotFound ex) {
            // 1. ejecutivo sin empresa
            return Optional.empty();
        } catch (WebClientException ex) {
            throw new ServicioNoDisponible();
        }
    }
}
