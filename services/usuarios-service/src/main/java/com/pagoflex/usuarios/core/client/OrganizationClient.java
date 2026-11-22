package com.pagoflex.usuarios.core.client;

import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import com.pagoflex.contracts.organizacion.RespuestaValidacionCodigoEmpresa;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidOrganizationCode;
import com.pagoflex.usuarios.features.auth.exceptions.OrganizationServiceUnavailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class OrganizationClient {
    private final WebClient client;

    public OrganizationClient(
            @Value("${cobranza-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String key) {
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", key)
                .build();
    }

    public RespuestaValidacionCodigoEmpresa validate(String codigo) {
        if (!SolicitudValidacionCodigoEmpresa.esValido(codigo)) {
            throw new InvalidOrganizationCode();
        }

        try {
            var result = client.post()
                    .uri("/codigos/validar")
                    .bodyValue(new SolicitudValidacionCodigoEmpresa(codigo))
                    .retrieve()
                    .onStatus(
                            status -> status.value() == 400
                                    || status.value() == 401
                                    || status.value() == 404,
                            response -> Mono.error(new InvalidOrganizationCode()))
                    .bodyToMono(RespuestaValidacionCodigoEmpresa.class)
                    .block(Duration.ofSeconds(10));

            if (result == null
                    || result.idEmpresa() == null
                    || result.nombreEmpresa() == null
                    || result.tipoEmpresa() == null
                    || result.rolAutorizado() == null) {
                throw new OrganizationServiceUnavailable();
            }
            return result;

        } catch (InvalidOrganizationCode | OrganizationServiceUnavailable ex) {
            throw ex;
        } catch (WebClientResponseException ex) {
            throw new OrganizationServiceUnavailable();
        } catch (RuntimeException ex) {
            throw new OrganizationServiceUnavailable();
        }
    }
}
