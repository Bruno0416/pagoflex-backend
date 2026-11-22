package com.pagoflex.cobranza.core.client;

import com.pagoflex.cobranza.core.exceptions.UsuariosNoDisponible;
import com.pagoflex.contracts.usuarios.SolicitudCrearPersonaInterna;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// Contrato de los clientes HTTP de cobranza con usuarios-service y reconocimientos-service
class ClientesInternosTest {
    private static final String CLAVE = "clave-de-servicio";
    private final UUID idPersona = UUID.randomUUID();
    private final UUID idUsuario = UUID.randomUUID();
    private final UUID idEmpresa = UUID.randomUUID();

    private HttpServer server;
    private String base;
    private volatile int status;
    private volatile String respuesta;
    private final AtomicReference<String> metodoYRuta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();
    private final AtomicReference<String> cuerpo = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws Exception {
        status = 200;
        respuesta = "{}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            metodoYRuta.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath());
            clave.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            cuerpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = respuesta.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void detener() {
        server.stop(0);
    }

    // ---------- usuarios ----------
    @Test
    void consultaPersonasPorLasRutasInternasDeUsuariosYMapeaAlContrato() {
        respuesta = """
                {"idPersona":"%s","rut":"15834207-3","nombres":"Camila","apellidos":"Rojas","correoContacto":"contacto@x.cl",
                 "telefono":"+56912345678","idUsuario":"%s","correoCuenta":"cuenta@x.cl","idEmpresa":"%s"}
                """.formatted(idPersona, idUsuario, idEmpresa);
        var cliente = usuarios();

        var persona = cliente.porUsuario(idUsuario).orElseThrow();
        assertThat(metodoYRuta.get()).isEqualTo("GET /api/v1/usuarios/internal/personas/por-usuario/" + idUsuario);
        assertThat(clave.get()).isEqualTo(CLAVE);
        // con cuenta, el correo es el de acceso
        assertThat(persona.id()).isEqualTo(idPersona);
        assertThat(persona.usuarioId()).isEqualTo(idUsuario);
        assertThat(persona.empresaId()).isEqualTo(idEmpresa);
        assertThat(persona.correo()).isEqualTo("cuenta@x.cl");

        cliente.porRut("15834207-3");
        assertThat(metodoYRuta.get()).isEqualTo("GET /api/v1/usuarios/internal/personas/por-rut/15834207-3");
        cliente.porId(idPersona);
        assertThat(metodoYRuta.get()).isEqualTo("GET /api/v1/usuarios/internal/personas/" + idPersona);
        cliente.porCorreo("cuenta@x.cl");
        assertThat(metodoYRuta.get()).isEqualTo("GET /api/v1/usuarios/internal/personas/por-correo/cuenta%40x.cl");
    }

    @Test
    void personaSinCuentaUsaElCorreoDeContactoYNoTieneUsuarioNiEmpresa() {
        respuesta = """
                {"idPersona":"%s","rut":"20111222-2","nombres":"N","apellidos":"A","correoContacto":"contacto@x.cl",
                 "telefono":"+56912345678"}
                """.formatted(idPersona);

        var persona = usuarios().porRut("20111222-2").orElseThrow();

        assertThat(persona.correo()).isEqualTo("contacto@x.cl");
        assertThat(persona.usuarioId()).isNull();
        assertThat(persona.empresaId()).isNull();
    }

    @Test
    void noEncontradaOInvalidaEsVaciaYUnaCaidaEsNoDisponible() {
        status = 404;
        assertThat(usuarios().porId(idPersona)).isEmpty();
        status = 400;
        assertThat(usuarios().porRut("mal")).isEmpty();
        status = 401;
        assertThatThrownBy(() -> usuarios().porId(idPersona)).isInstanceOf(UsuariosNoDisponible.class);
        status = 503;
        assertThatThrownBy(() -> usuarios().porUsuario(idUsuario)).isInstanceOf(UsuariosNoDisponible.class);
    }

    @Test
    void creaLaPersonaSinCuentaEnLaRutaDeUsuariosYLimpiaElTelefono() {
        status = 201;
        respuesta = """
                {"idPersona":"%s","rut":"20111222-2","nombres":"Ana","apellidos":"Paz","correoContacto":"ana@x.cl",
                 "telefono":"+56900001000"}
                """.formatted(idPersona);

        var creada = usuarios().crear(new SolicitudCrearPersonaInterna("20111222-2", "Ana", "Paz", "ana@x.cl", "+56 9 0000 1000"));

        assertThat(metodoYRuta.get()).isEqualTo("POST /api/v1/usuarios/internal/personas");
        assertThat(cuerpo.get()).contains("\"rut\":\"20111222-2\"", "\"correoContacto\":\"ana@x.cl\"",
                "\"telefono\":\"+56900001000\"");
        assertThat(creada.id()).isEqualTo(idPersona);
    }

    @Test
    void siLaPersonaYaExisteTomaLaExistente() {
        // el POST responde 409 y la segunda consulta trae la persona
        server.removeContext("/");
        server.createContext("/", exchange -> {
            boolean post = exchange.getRequestMethod().equals("POST");
            byte[] bytes = (post ? "{}" : """
                    {"idPersona":"%s","rut":"20111222-2","nombres":"Ana","apellidos":"Paz","telefono":"+56900001000"}
                    """.formatted(idPersona)).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(post ? 409 : 200, bytes.length);
            try (var out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });

        var persona = usuarios().crear(new SolicitudCrearPersonaInterna("20111222-2", "Ana", "Paz", null, "+56900001000"));

        assertThat(persona.id()).isEqualTo(idPersona);
    }

    // ---------- reconocimientos ----------
    @Test
    void pideLaEvaluacionConLaClaveInternaYElUsuario() {
        respuesta = "{\"idUsuario\":\"" + idUsuario + "\",\"otorgados\":[],\"evaluadoEn\":\"2026-10-06T10:00:00Z\"}";

        boolean ok = reconocimientos(base + "/api/v1/internal/reconocimientos").evaluar(idUsuario, null);

        assertThat(ok).isTrue();
        assertThat(metodoYRuta.get()).isEqualTo("POST /api/v1/internal/reconocimientos/evaluar");
        assertThat(clave.get()).isEqualTo(CLAVE);
        assertThat(cuerpo.get()).contains("\"idUsuario\":\"" + idUsuario + "\"");
    }

    @Test
    void siReconocimientosFallaNoLanzaExcepcion() {
        status = 503;
        assertThat(reconocimientos(base + "/api/v1/internal/reconocimientos").evaluar(idUsuario, UUID.randomUUID()))
                .isFalse();
        // y tampoco si no responde
        assertThat(reconocimientos("http://127.0.0.1:1/api/v1/internal/reconocimientos").evaluar(idUsuario, null))
                .isFalse();
    }

    // --- helpers ---
    private UsuariosServiceClient usuarios() {
        return new UsuariosServiceClient(WebClient.builder(), base + "/api/v1/usuarios/internal", CLAVE);
    }

    private ReconocimientosClient reconocimientos(String url) {
        return new ReconocimientosClient(url, CLAVE);
    }
}
