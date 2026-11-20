package com.pagoflex.usuarios.core.client;

import com.pagoflex.contracts.organization.AuthorizedRole;
import com.pagoflex.contracts.organization.OrganizationType;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidOrganizationCode;
import com.pagoflex.usuarios.features.auth.exceptions.OrganizationServiceUnavailable;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class OrganizationClientTest {
    private HttpServer server;
    private OrganizationClient client;
    private volatile int status;
    private volatile String body;
    private volatile String receivedKey;
    private volatile String receivedBody;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        status = 200;
        body = "{}";
        receivedBody = null;
        server.createContext("/codigos/validar", exchange -> {
            receivedKey = exchange.getRequestHeaders().getFirst("X-Internal-Api-Key");
            receivedBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var stream = exchange.getResponseBody()) { stream.write(bytes); }
        });
        server.start();
        client = new OrganizationClient("http://127.0.0.1:" + server.getAddress().getPort(), "clave-de-servicio");
    }

    @AfterEach
    void stop() { server.stop(0); }

    // ---------- contrato de cobranza ----------
    @Test
    void recognizesValidOrganizationAndUsesInternalKey() {
        UUID id = UUID.randomUUID();
        body = "{\"idEmpresa\":\"" + id
                + "\",\"nombreEmpresa\":\"Empresa demo\",\"tipoEmpresa\":\"CLIENTE\",\"rolAutorizado\":\"EJECUTIVO\"}";
        var response = client.validate("CL34EX-ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        assertThat(response.idEmpresa()).isEqualTo(id);
        assertThat(response.tipoEmpresa()).isEqualTo(OrganizationType.CLIENTE);
        assertThat(response.rolAutorizado()).isEqualTo(AuthorizedRole.EJECUTIVO);
        assertThat(receivedKey).isEqualTo("clave-de-servicio");
        assertThat(receivedBody).contains("\"codigo\":\"CL34EX-ABCDEFGHIJKLMNOPQRSTUVWXYZ\"");
        assertThat(receivedBody).doesNotContain("hash", "rolAutorizado");
    }

    @Test
    void rejectsInvalidFormatWithoutSendingHttpRequest() {
        for (String code : new String[]{"AB1234", "AA00AA", "aa00aa-ABCDEFGHIJKLMNOPQRSTUVWXYZ", "AA00AA-SHORT", "AA00AA-ABCDEFGHIJKLMNOPQRSTUVWXY1", "  "}) {
            assertThatThrownBy(() -> client.validate(code))
                    .isInstanceOf(InvalidOrganizationCode.class);
        }
        assertThat(receivedBody).isNull();
    }

    @Test
    void rejectsInvalidCodeFromCobranza() {
        status = 401;
        assertThatThrownBy(() -> client.validate("CL34EX-ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                .isInstanceOf(InvalidOrganizationCode.class);
    }

    @Test
    void failsClosedForUnavailableCobranza() {
        status = 503;
        assertThatThrownBy(() -> client.validate("CL34EX-ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                .isInstanceOf(OrganizationServiceUnavailable.class);
    }

    @Test
    void rejectsIncompleteResponse() {
        assertThatThrownBy(() -> client.validate("CL34EX-ABCDEFGHIJKLMNOPQRSTUVWXYZ"))
                .isInstanceOf(OrganizationServiceUnavailable.class);
    }
}
