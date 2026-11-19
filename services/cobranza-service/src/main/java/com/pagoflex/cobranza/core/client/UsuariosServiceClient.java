package com.pagoflex.cobranza.core.client;

import com.pagoflex.cobranza.core.exceptions.UsuariosNoDisponible;
import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/// Personas y cuentas viven en usuarios-service; aquí solo se consultan por la ruta interna
@Component
public class UsuariosServiceClient {
    private static final Duration ESPERA = Duration.ofSeconds(8);
    private final WebClient client;

    public UsuariosServiceClient(
            WebClient.Builder builder,
            @Value("${usuarios-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. cliente con la clave interna en cada llamada
        this.client = builder
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    // ---------- consultas ----------
    public Optional<PersonaInterna> porUsuario(UUID usuarioId) {
        return buscar(() -> get("/personas/usuario/{id}", usuarioId));
    }

    public Optional<PersonaInterna> porRut(String rut) {
        return buscar(() -> get("/personas/rut/{rut}", rut));
    }

    public Optional<PersonaInterna> porId(UUID id) {
        return buscar(() -> get("/personas/{id}", id));
    }

    public Optional<PersonaInterna> porCorreo(String correo) {
        return buscar(() -> get("/usuarios/correo/{correo}", correo));
    }

    // ---------- alta RN-20 ----------
    public PersonaInterna crear(CrearPersonaInterna request) {
        try {
            return client.post().uri("/personas").bodyValue(request)
                    .retrieve().bodyToMono(PersonaInterna.class).block(ESPERA);
        } catch (WebClientResponseException.Conflict ex) {
            // 1. alguien la creó recién: usar la existente
            return porRut(request.rut()).orElseThrow(UsuariosNoDisponible::new);
        } catch (WebClientException ex) {
            throw new UsuariosNoDisponible();
        }
    }

    // --- helpers privados ---
    private PersonaInterna get(String uri, Object variable) {
        return client.get().uri(uri, variable).retrieve().bodyToMono(PersonaInterna.class).block(ESPERA);
    }

    private Optional<PersonaInterna> buscar(Supplier<PersonaInterna> llamada) {
        try {
            return Optional.ofNullable(llamada.get());
        } catch (WebClientResponseException ex) {
            // 1. no existe o el dato no es válido: se trata como no encontrada
            HttpStatusCode status = ex.getStatusCode();
            if (status.value() == 404 || status.value() == 400) return Optional.empty();
            throw new UsuariosNoDisponible();
        } catch (WebClientException ex) {
            // 2. usuarios caído o lento
            throw new UsuariosNoDisponible();
        }
    }
}
