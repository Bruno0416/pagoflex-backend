package com.pagoflex.cobranza.core.client;

import com.pagoflex.cobranza.core.exceptions.UsuariosNoDisponible;
import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.security.InternalApiKeyFilter;
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

/// Personas y cuentas viven en usuarios-service; aquí solo se consultan por su ruta interna
/// (/api/v1/usuarios/internal/personas) y se mapean al contrato PersonaInterna.
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
                .defaultHeader(InternalApiKeyFilter.HEADER, internalKey)
                .build();
    }

    // ---------- consultas ----------
    public Optional<PersonaInterna> porUsuario(UUID usuarioId) {
        return buscar(() -> get("/personas/por-usuario/{id}", usuarioId));
    }

    public Optional<PersonaInterna> porRut(String rut) {
        return buscar(() -> get("/personas/por-rut/{rut}", rut));
    }

    public Optional<PersonaInterna> porId(UUID id) {
        return buscar(() -> get("/personas/{id}", id));
    }

    public Optional<PersonaInterna> porCorreo(String correo) {
        return buscar(() -> get("/personas/por-correo/{correo}", correo));
    }

    // ---------- alta RN-20 ----------
    public PersonaInterna crear(CrearPersonaInterna request) {
        try {
            PersonaRespuesta creada = client.post().uri("/personas")
                    .bodyValue(new CrearPersonaSolicitud(request.rut(), request.nombres(), request.apellidos(),
                            request.correo(), limpiarTelefono(request.telefono())))
                    .retrieve().bodyToMono(PersonaRespuesta.class).block(ESPERA);
            if (creada == null) throw new UsuariosNoDisponible();
            return creada.aContrato();
        } catch (WebClientResponseException.Conflict ex) {
            // 1. alguien la creó recién: usar la existente
            return porRut(request.rut()).orElseThrow(UsuariosNoDisponible::new);
        } catch (WebClientException ex) {
            throw new UsuariosNoDisponible();
        }
    }

    // --- helpers privados ---
    private PersonaInterna get(String uri, Object variable) {
        PersonaRespuesta respuesta = client.get().uri(uri, variable).retrieve()
                .bodyToMono(PersonaRespuesta.class).block(ESPERA);
        return respuesta == null ? null : respuesta.aContrato();
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

    // usuarios guarda el teléfono sin espacios ni signos de formato
    private static String limpiarTelefono(String telefono) {
        return telefono == null ? null : telefono.replaceAll("[\\s()\\-]", "");
    }

    // ---------- formato de usuarios-service ----------
    private record CrearPersonaSolicitud(String rut, String nombres, String apellidos,
                                         String correoContacto, String telefono) {
    }

    private record PersonaRespuesta(UUID idPersona, String rut, String nombres, String apellidos,
                                    String correoContacto, String telefono, UUID idUsuario,
                                    String correoCuenta, UUID idEmpresa) {
        // con cuenta el correo es el de acceso; sin cuenta, el de contacto
        PersonaInterna aContrato() {
            return new PersonaInterna(idPersona, rut, nombres, apellidos,
                    correoCuenta != null ? correoCuenta : correoContacto, telefono, idUsuario, idEmpresa);
        }
    }
}
