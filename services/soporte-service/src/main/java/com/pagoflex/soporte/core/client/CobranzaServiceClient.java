package com.pagoflex.soporte.core.client;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.soporte.core.exceptions.CobranzaNoDisponible;
import com.pagoflex.soporte.core.exceptions.CobroNoReportable;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.core.exceptions.EjecutivoSinEmpresa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class CobranzaServiceClient {
    private static final Duration ESPERA = Duration.ofSeconds(10);

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

    // datos del cobro y si este usuario lo puede reportar; usuarioId puede ir nulo
    public CompromisoInterno reportabilidad(String folio, UUID usuarioId) {
        try {
            return client.get()
                    .uri(b -> {
                        b.path("/compromisos/{folio}/reportabilidad");
                        if (usuarioId != null) b.queryParam("usuarioId", usuarioId);
                        return b.build(folio);
                    })
                    .retrieve()
                    .onStatus(s -> s.value() == 404, r -> Mono.error(new CompromisoNoEncontrado(folio)))
                    .bodyToMono(CompromisoInterno.class)
                    .block(ESPERA);
        } catch (WebClientException ex) {
            throw new CobranzaNoDisponible();
        }
    }

    public CompromisoInterno marcarEnRevision(String folio) {
        return cambiar("/compromisos/{folio}/revision", folio);
    }

    public CompromisoInterno finRevision(String folio) {
        return cambiar("/compromisos/{folio}/fin-revision", folio);
    }

    // empresa del ejecutivo; 404 en cobranza significa que no tiene una asignada
    public EmpresaInterna empresaDeEjecutivo(UUID usuarioId) {
        try {
            return client.get()
                    .uri("/empresas/ejecutivos/{usuarioId}", usuarioId)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, r -> Mono.error(new EjecutivoSinEmpresa()))
                    .bodyToMono(EmpresaInterna.class)
                    .block(ESPERA);
        } catch (WebClientException ex) {
            throw new CobranzaNoDisponible();
        }
    }

    // --- helper privado ---
    private CompromisoInterno cambiar(String uri, String folio) {
        try {
            // 1. pedir el cambio y traducir los errores de cobranza
            return client.method(HttpMethod.PUT)
                    .uri(uri, folio)
                    .retrieve()
                    .onStatus(s -> s.value() == 404, r -> Mono.error(new CompromisoNoEncontrado(folio)))
                    .onStatus(s -> s.value() == 409, r -> r.bodyToMono(Map.class)
                            .map(body -> new CobroNoReportable(String.valueOf(
                                    body.getOrDefault("detail", "Este cobro no se puede reportar")))))
                    .bodyToMono(CompromisoInterno.class)
                    .block(ESPERA);
        } catch (WebClientException ex) {
            // 2. cobranza caído o lento: avisar sin exponer el error interno
            throw new CobranzaNoDisponible();
        }
    }
}
