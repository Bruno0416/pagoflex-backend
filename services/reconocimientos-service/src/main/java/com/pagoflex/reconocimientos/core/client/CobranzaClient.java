package com.pagoflex.reconocimientos.core.client;

import com.pagoflex.contracts.cobranza.PaymentFactsResponse;
import com.pagoflex.security.InternalApiKeyFilter;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaInvalidData;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaUnavailable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


@Slf4j
@Component
public class CobranzaClient {
    private final RestClient client;

    public CobranzaClient(@Value("${pagoflex.reconocimientos.cobranza.base-url}") String baseUrl,
                          @Value("${pagoflex.security.internal-api-key}") String key) {
        // 1. fijar la url de cobranza y adjuntar la clave de servicio a toda peticion
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, key).build();
    }

    // ---------- consulta de hechos de pago ----------
    public PaymentFactsResponse fetchPaymentFacts(UUID userId) {
        try {
            // 1. consultar el historial completo del usuario
            PaymentFactsResponse body = client.get().uri("/usuarios/{id}/hechos-pago", userId)
                    .retrieve().body(PaymentFactsResponse.class);
            if (body == null) throw new CobranzaInvalidData();
            return body;
        } catch (HttpClientErrorException.NotFound ex) {
            // 2. un usuario sin compromisos equivale a un historial vacio
            return new PaymentFactsResponse(userId, OffsetDateTime.now(), List.of());
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // 3. rechazo o caida: cobranza no esta disponible para esta operacion
            log.warn("cobranza respondio estado {}", ex.getStatusCode().value());
            throw new CobranzaUnavailable();
        } catch (ResourceAccessException ex) {
            // 4. conexion rechazada o sin respuesta
            log.warn("cobranza no accesible: {}", ex.getClass().getSimpleName());
            throw new CobranzaUnavailable();
        } catch (RestClientException ex) {
            // 5. cuerpo ilegible o con valores desconocidos: falla cerrada
            log.warn("respuesta de cobranza ilegible: {}", ex.getClass().getSimpleName());
            throw new CobranzaInvalidData();
        }
    }
}
