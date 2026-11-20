package com.pagoflex.cobranza.core.client;

import com.pagoflex.cobranza.core.exceptions.UsuariosNoDisponible;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/// Personas y cuentas viven en usuarios-service; se consultan por sus rutas internas
@Component
public class UsuariosServiceClient {
    private static final Duration ESPERA = Duration.ofSeconds(8);
    private final WebClient client;

    /// respuesta de /personas/por-rut y /personas/{id}
    record Persona(UUID idPersona, String rut, String nombres, String apellidos, String correoContacto,
                   String telefono) {
        PersonaInterna aInterna() {
            return new PersonaInterna(idPersona, rut, nombres, apellidos, correoContacto, telefono, null, null);
        }
    }

    /// respuesta de /personas/por-usuario y /personas/por-correo
    record Cuenta(UUID idUsuario, UUID idPersona, String rut, String nombres, String apellidos, String correo,
                  String telefono, UUID idEmpresa, String rol) {
        PersonaInterna aInterna() {
            return new PersonaInterna(idPersona, rut, nombres, apellidos, correo, telefono, idUsuario, idEmpresa);
        }
    }

    /// cuerpo que espera usuarios para crear una persona sin cuenta
    record NuevaPersona(String rut, String nombres, String apellidos, String correoContacto, String telefono) {
    }

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
        return buscar(() -> cuenta("/personas/por-usuario/{id}", usuarioId));
    }

    public Optional<PersonaInterna> porCorreo(String correo) {
        return buscar(() -> cuenta("/personas/por-correo/{correo}", correo));
    }

    public Optional<PersonaInterna> porRut(String rut) {
        return buscar(() -> persona("/personas/por-rut/{rut}", rut));
    }

    public Optional<PersonaInterna> porId(UUID id) {
        return buscar(() -> persona("/personas/{id}", id));
    }

    // ---------- alta RN-20 ----------
    public PersonaInterna crear(CrearPersonaInterna request) {
        // 1. usuarios guarda el teléfono solo con dígitos y el signo +
        NuevaPersona cuerpo = new NuevaPersona(request.rut(), request.nombres(), request.apellidos(),
                request.correo(), request.telefono().replaceAll("[\\s-]", ""));
        try {
            Persona creada = client.post().uri("/personas").bodyValue(cuerpo)
                    .retrieve().bodyToMono(Persona.class).block(ESPERA);
            if (creada == null) throw new UsuariosNoDisponible();
            return creada.aInterna();
        } catch (WebClientResponseException.Conflict ex) {
            // 2. alguien la creó recién: usar la existente
            return porRut(request.rut()).orElseThrow(UsuariosNoDisponible::new);
        } catch (WebClientException ex) {
            throw new UsuariosNoDisponible();
        }
    }

    // --- helpers privados ---
    private PersonaInterna persona(String uri, Object variable) {
        Persona p = client.get().uri(uri, variable).retrieve().bodyToMono(Persona.class).block(ESPERA);
        return p == null ? null : p.aInterna();
    }

    private PersonaInterna cuenta(String uri, Object variable) {
        Cuenta c = client.get().uri(uri, variable).retrieve().bodyToMono(Cuenta.class).block(ESPERA);
        return c == null ? null : c.aInterna();
    }

    private Optional<PersonaInterna> buscar(Supplier<PersonaInterna> llamada) {
        try {
            return Optional.ofNullable(llamada.get());
        } catch (WebClientResponseException ex) {
            // 1. no existe o el dato no es válido: se trata como no encontrada
            int status = ex.getStatusCode().value();
            if (status == 404 || status == 400) return Optional.empty();
            throw new UsuariosNoDisponible();
        } catch (WebClientException ex) {
            // 2. usuarios caído o lento
            throw new UsuariosNoDisponible();
        }
    }
}
