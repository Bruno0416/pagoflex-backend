package com.pagoflex.soporte.core.client;

import com.pagoflex.contracts.usuarios.RespuestaPersonaInterna;

import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.security.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;

/// Solo lo usa la carga demo para conocer el uuid de las cuentas de ejemplo; consulta la ruta interna de personas
@Component
public class UsuariosServiceClient {
    private final WebClient client;

    public UsuariosServiceClient(
            WebClient.Builder builder,
            @Value("${usuarios-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. configurar el cliente http y su clave interna
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader(InternalApiKeyFilter.HEADER, internalKey)
                .build();
    }

    // persona con cuenta por su correo; nulo si no existe
    public PersonaInterna porCorreo(String correo) {
        RespuestaPersonaInterna respuesta = client.get()
                .uri("/personas/por-correo/{correo}", correo)
                .retrieve()
                .bodyToMono(RespuestaPersonaInterna.class)
                // 404: la cuenta no existe
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.empty())
                .block(Duration.ofSeconds(10));
        return respuesta == null ? null : respuesta.aPersonaInterna();
    }
}
