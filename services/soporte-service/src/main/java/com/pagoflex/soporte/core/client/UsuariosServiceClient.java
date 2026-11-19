package com.pagoflex.soporte.core.client;

import com.pagoflex.contracts.usuarios.PersonaInterna;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

/// Solo lo usa la carga demo para conocer el uuid de las cuentas de ejemplo
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
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    // persona con cuenta por su correo; nulo si no existe
    public PersonaInterna porCorreo(String correo) {
        return client.get()
                .uri("/usuarios/correo/{correo}", correo)
                .retrieve()
                .onStatus(s -> s.value() == 404, r -> Mono.empty())
                .bodyToMono(PersonaInterna.class)
                .block(Duration.ofSeconds(10));
    }
}
