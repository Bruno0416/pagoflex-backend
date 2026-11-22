package com.pagoflex.soporte.core.client;

import com.pagoflex.soporte.core.exceptions.CobranzaNoDisponible;
import com.pagoflex.soporte.core.exceptions.CobroNoReportable;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.core.exceptions.EjecutivoSinEmpresa;
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

/// Contrato de los clientes HTTP de soporte con usuarios-service y cobranza-service
class ClientesInternosTest {
    private static final String CLAVE = "clave-de-servicio";
    private final UUID idPersona = UUID.randomUUID();
    private final UUID idUsuario = UUID.randomUUID();

    private HttpServer server;
    private String base;
    private volatile int status;
    private volatile String respuesta;
    private final AtomicReference<String> metodoYRuta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws Exception {
        status = 200;
        respuesta = "{}";
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            metodoYRuta.set(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery()));
            clave.set(exchange.getRequestHeaders().getFirst("X-Internal-Api-Key"));
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
    void usuariosSeConsultaPorLaRutaInternaDePersonasYMapeaAlContrato() {
        respuesta = """
                {"idPersona":"%s","rut":"15834207-3","nombres":"Héctor","apellidos":"Pizarro","correoContacto":"c@x.cl",
                 "telefono":"+56900000106","idUsuario":"%s","correoCuenta":"hpizarro@correo.ejemplo"}
                """.formatted(idPersona, idUsuario);

        var persona = new UsuariosServiceClient(WebClient.builder(), base + "/api/v1/usuarios/internal", CLAVE)
                .porCorreo("hpizarro@correo.ejemplo");

        assertThat(metodoYRuta.get())
                .isEqualTo("GET /api/v1/usuarios/internal/personas/por-correo/hpizarro%40correo.ejemplo");
        assertThat(clave.get()).isEqualTo(CLAVE);
        assertThat(persona.usuarioId()).isEqualTo(idUsuario);
        assertThat(persona.correo()).isEqualTo("hpizarro@correo.ejemplo");
    }

    @Test
    void usuariosSinCuentaEncontradaDevuelveNulo() {
        status = 404;
        assertThat(new UsuariosServiceClient(WebClient.builder(), base + "/api/v1/usuarios/internal", CLAVE)
                .porCorreo("nadie@x.cl")).isNull();
    }

    // ---------- cobranza ----------
    @Test
    void cobranzaReportabilidadYRevisionUsanLasRutasInternas() {
        respuesta = """
                {"folio":"CP-2026-0001","reportable":true,"delUsuario":true,"estado":"PENDIENTE","total":1000}
                """;
        var cliente = cobranza();

        var cobro = cliente.reportabilidad("CP-2026-0001", idUsuario);
        assertThat(metodoYRuta.get())
                .isEqualTo("GET /api/v1/internal/compromisos/CP-2026-0001/reportabilidad?usuarioId=" + idUsuario);
        assertThat(clave.get()).isEqualTo(CLAVE);
        assertThat(cobro.reportable()).isTrue();

        cliente.marcarEnRevision("CP-2026-0001");
        assertThat(metodoYRuta.get()).isEqualTo("PUT /api/v1/internal/compromisos/CP-2026-0001/revision");
        cliente.finRevision("CP-2026-0001");
        assertThat(metodoYRuta.get()).isEqualTo("PUT /api/v1/internal/compromisos/CP-2026-0001/fin-revision");
    }

    @Test
    void cobranzaEmpresaDelEjecutivoYSusErrores() {
        UUID empresa = UUID.randomUUID();
        respuesta = "{\"id\":\"" + empresa + "\",\"codigo\":\"EC-01\",\"nombre\":\"Altos del Maipo\"}";

        var interna = cobranza().empresaDeEjecutivo(idUsuario);
        assertThat(metodoYRuta.get()).isEqualTo("GET /api/v1/internal/empresas/ejecutivos/" + idUsuario);
        assertThat(interna.id()).isEqualTo(empresa);
        assertThat(interna.codigo()).isEqualTo("EC-01");

        status = 404;
        assertThatThrownBy(() -> cobranza().empresaDeEjecutivo(idUsuario)).isInstanceOf(EjecutivoSinEmpresa.class);
        assertThatThrownBy(() -> cobranza().marcarEnRevision("CP-1")).isInstanceOf(CompromisoNoEncontrado.class);
        status = 409;
        respuesta = "{\"detail\":\"Este cobro está pagado\"}";
        assertThatThrownBy(() -> cobranza().marcarEnRevision("CP-1")).isInstanceOf(CobroNoReportable.class)
                .hasMessageContaining("pagado");
        status = 503;
        assertThatThrownBy(() -> cobranza().reportabilidad("CP-1", null)).isInstanceOf(CobranzaNoDisponible.class);
    }

    // --- helper ---
    private CobranzaServiceClient cobranza() {
        return new CobranzaServiceClient(WebClient.builder(), base + "/api/v1/internal", CLAVE);
    }
}
