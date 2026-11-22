package com.pagoflex.mensajeria.core;

import com.pagoflex.mensajeria.core.client.CobranzaServiceClient;
import com.pagoflex.mensajeria.core.client.SoporteServiceClient;
import com.pagoflex.mensajeria.core.exceptions.ServicioNoDisponible;
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

/// Contrato de los clientes de mensajería con soporte (participantes) y cobranza (empresa del ejecutivo)
class ClientesInternosTest {
    private static final String CLAVE = "clave-de-servicio";
    private HttpServer server;
    private String base;
    private volatile int status;
    private volatile String respuesta;
    private final AtomicReference<String> ruta = new AtomicReference<>();
    private final AtomicReference<String> clave = new AtomicReference<>();

    @BeforeEach
    void iniciar() throws Exception {
        status = 200;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ruta.set(ex.getRequestMethod() + " " + ex.getRequestURI().getRawPath());
            clave.set(ex.getRequestHeaders().getFirst("X-Internal-Api-Key"));
            byte[] b = respuesta.getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().add("Content-Type", "application/json");
            ex.sendResponseHeaders(status, b.length);
            try (var out = ex.getResponseBody()) {
                out.write(b);
            }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/internal";
    }

    @AfterEach
    void detener() {
        server.stop(0);
    }

    @Test
    void participantesDeSoporteConLosCamposReales() {
        UUID empresa = UUID.randomUUID(), agente = UUID.randomUUID(), usuario = UUID.randomUUID();
        // mismo JSON que ParticipantesReporteInterno de soporte (estado como nombre del enum)
        respuesta = """
                {"numero":"REP-0013","folio":"CP-2026-0106","estado":"EN_ATENCION","empresaId":"%s","agenteId":"%s","usuarioId":"%s"}
                """.formatted(empresa, agente, usuario);

        var p = new SoporteServiceClient(WebClient.builder(), base, CLAVE).participantes("REP-0013").orElseThrow();

        assertThat(ruta.get()).isEqualTo("GET /api/v1/internal/reportes/REP-0013/participantes");
        assertThat(clave.get()).isEqualTo(CLAVE);
        assertThat(p.estado()).isEqualTo("EN_ATENCION");
        assertThat(p.empresaId()).isEqualTo(empresa);
        assertThat(p.agenteId()).isEqualTo(agente);
        assertThat(p.usuarioId()).isEqualTo(usuario);
    }

    @Test
    void soporteNoEncontradoOCaido() {
        respuesta = "{}";
        status = 404;
        assertThat(new SoporteServiceClient(WebClient.builder(), base, CLAVE).participantes("X")).isEmpty();
        status = 503;
        assertThatThrownBy(() -> new SoporteServiceClient(WebClient.builder(), base, CLAVE).participantes("X"))
                .isInstanceOf(ServicioNoDisponible.class);
    }

    @Test
    void empresaDelEjecutivoDeCobranza() {
        UUID empresa = UUID.randomUUID(), ejecutivo = UUID.randomUUID();
        respuesta = "{\"id\":\"" + empresa + "\",\"codigo\":\"EC-01\",\"nombre\":\"Altos del Maipo\"}";

        var e = new CobranzaServiceClient(WebClient.builder(), base, CLAVE).empresaDeEjecutivo(ejecutivo).orElseThrow();

        assertThat(ruta.get()).isEqualTo("GET /api/v1/internal/empresas/ejecutivos/" + ejecutivo);
        assertThat(clave.get()).isEqualTo(CLAVE);
        assertThat(e.id()).isEqualTo(empresa);

        status = 404;
        assertThat(new CobranzaServiceClient(WebClient.builder(), base, CLAVE).empresaDeEjecutivo(ejecutivo)).isEmpty();
        status = 500;
        assertThatThrownBy(() -> new CobranzaServiceClient(WebClient.builder(), base, CLAVE).empresaDeEjecutivo(ejecutivo))
                .isInstanceOf(ServicioNoDisponible.class);
    }
}
