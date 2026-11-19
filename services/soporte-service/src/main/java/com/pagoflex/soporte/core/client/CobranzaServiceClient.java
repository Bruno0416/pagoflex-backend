package com.pagoflex.soporte.core.client;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.soporte.core.exceptions.CobranzaNoDisponible;
import com.pagoflex.soporte.core.exceptions.CobroNoReportable;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
public class CobranzaServiceClient {
    private final WebClient client;

    // ---------- comunicación interna ----------
    public CobranzaServiceClient(
            WebClient.Builder builder,
            @Value("${cobranza-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. configurar el cliente http y su clave interna
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    public CompromisoInterno obtener(String folio) {
        return llamar(HttpMethod.GET, "/compromisos/{folio}", folio);
    }

    public CompromisoInterno marcarEnRevision(String folio) {
        return llamar(HttpMethod.POST, "/compromisos/{folio}/revision", folio);
    }

    public CompromisoInterno salirDeRevision(String folio) {
        return llamar(HttpMethod.DELETE, "/compromisos/{folio}/revision", folio);
    }

    // --- helper privado ---
    private CompromisoInterno llamar(HttpMethod metodo, String uri, String folio) {
        try {
            // 1. pedir la operación y traducir los errores de cobranza
            return client.method(metodo)
                    .uri(uri, folio)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, r -> Mono.error(new CompromisoNoEncontrado(folio)))
                    .onStatus(s -> s.value() == 409, r -> r.bodyToMono(Map.class)
                            .map(body -> new CobroNoReportable(String.valueOf(
                                    body.getOrDefault("detail", "Este cobro no se puede reportar")))))
                    .bodyToMono(CompromisoInterno.class)
                    .block(Duration.ofSeconds(10));
        } catch (WebClientException ex) {
            // 2. cobranza caído o lento: avisar sin exponer el error interno
            throw new CobranzaNoDisponible();
        }
    }
}
