package com.pagoflex.soporte.core.client;

import com.pagoflex.soporte.core.client.PersonaInterna;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

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

    /// respuesta de /personas/por-correo de usuarios-service
    record Cuenta(UUID idUsuario, UUID idPersona, String rut, String nombres, String apellidos, String correo,
                  String telefono, UUID idEmpresa, String rol) {
    }

    // persona con cuenta por su correo; nulo si no existe
    public PersonaInterna porCorreo(String correo) {
        Cuenta c = client.get()
                .uri("/personas/por-correo/{correo}", correo)
                .retrieve()
                .onStatus(s -> s.value() == 404, r -> Mono.empty())
                .bodyToMono(Cuenta.class)
                .block(Duration.ofSeconds(10));
        return c == null ? null : new PersonaInterna(c.idPersona(), c.rut(), c.nombres(), c.apellidos(), c.correo(),
                c.telefono(), c.idUsuario(), c.idEmpresa());
    }
}
