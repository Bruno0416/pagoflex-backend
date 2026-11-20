package com.pagoflex.usuarios.core.client;

import com.pagoflex.contracts.organization.ValidateOrganizationCodeRequest;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeResponse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidOrganizationCode;
import com.pagoflex.usuarios.features.auth.exceptions.OrganizationServiceUnavailable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class OrganizationClient {
    private final RestClient client;

    public OrganizationClient(@Value("${cobranza-service.api.base-url}") String baseUrl,
                              @Value("${pagoflex.security.internal-api-key}") String key) {
        this.client = RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", key).build();
    }

    // ---------- validacion de codigo ----------
    public ValidateOrganizationCodeResponse validate(String codigo) {
        // 1. rechazar formatos invalidos antes de consultar cobranza
        if (!ValidateOrganizationCodeRequest.esValido(codigo)) throw new InvalidOrganizationCode();
        try {
            // 2. consultar la organizacion antes de crear la cuenta
            var result = client.post().uri("/codigos/validar")
                    .body(new ValidateOrganizationCodeRequest(codigo))
                    .retrieve()
                    .onStatus(status -> status.value() == 400 || status.value() == 401 || status.value() == 404,
                            (request, response) -> { throw new InvalidOrganizationCode(); })
                    .body(ValidateOrganizationCodeResponse.class);
            if (result == null || result.idEmpresa() == null || result.tipoEmpresa() == null
                    || result.rolAutorizado() == null || result.nombreEmpresa() == null) {
                throw new OrganizationServiceUnavailable();
            }
            return result;
        } catch (InvalidOrganizationCode | OrganizationServiceUnavailable ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new OrganizationServiceUnavailable();
        }
    }
}
