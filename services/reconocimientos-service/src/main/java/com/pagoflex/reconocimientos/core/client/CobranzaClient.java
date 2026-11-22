package com.pagoflex.reconocimientos.core.client;

import com.pagoflex.contracts.cobranza.RespuestaHechosPago;
import com.pagoflex.security.InternalApiKeyFilter;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaInvalidData;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaUnavailable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.codec.CodecException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class CobranzaClient {
    private final WebClient client;

    public CobranzaClient(
            @Value("${pagoflex.reconocimientos.cobranza.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String key) {
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, key)
                .build();
    }

    public RespuestaHechosPago fetchPaymentFacts(UUID userId) {
        try {
            var result = client.get()
                    .uri("/usuarios/{id}/hechos-pago", userId)
                    .retrieve()
                    .bodyToMono(RespuestaHechosPago.class)
                    .block(Duration.ofSeconds(10));

            if (result == null) {
                throw new CobranzaInvalidData();
            }
            return result;

        } catch (WebClientResponseException.NotFound ex) {
            return new RespuestaHechosPago(
                    userId, OffsetDateTime.now(), List.of());
        } catch (WebClientResponseException ex) {
            log.warn("Cobranza respondió HTTP {}", ex.getStatusCode().value());
            throw new CobranzaUnavailable();
        } catch (CodecException ex) {
            log.warn("Respuesta de Cobranza ilegible");
            throw new CobranzaInvalidData();
        } catch (CobranzaInvalidData ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Cobranza no disponible: {}",
                    ex.getClass().getSimpleName());
            throw new CobranzaUnavailable();
        }
    }
}
