package com.pagoflex.reconocimientos.core.client;

import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaInvalidData;
import com.pagoflex.reconocimientos.core.exceptions.CobranzaUnavailable;
import com.pagoflex.reconocimientos.support.CobranzaStubServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CobranzaClientTest {
    private static final UUID USER = UUID.fromString("3f1c2b9e-6a1d-4c39-9b55-0d6f6f1f2a10");

    private CobranzaStubServer server;
    private CobranzaClient client;

    @BeforeEach
    void start() {
        server = new CobranzaStubServer();
        client = new CobranzaClient(server.baseUrl(), "clave-de-servicio");
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    // ---------- contrato ----------
    @Test
    void sendsInternalKeyAndParsesCanonicalFixture() throws IOException {
        server.respond(200, fixture());
        var facts = client.fetchPaymentFacts(USER);
        assertThat(server.receivedKey).isEqualTo("clave-de-servicio");
        assertThat(server.receivedPath).isEqualTo("/api/v1/internal/usuarios/" + USER + "/hechos-pago");
        assertThat(facts.idUsuario()).isEqualTo(USER);
        assertThat(facts.compromisos()).hasSize(5);
        assertThat(facts.compromisos().get(0).estado()).isEqualTo(EstadoCompromisoEvaluacion.PAGADO);
        assertThat(facts.compromisos().get(0).pago().numeroComprobante()).isEqualTo("CMP-2026-0101");
        assertThat(facts.compromisos().get(2).pago()).isNull();
    }

    @Test
    void unknownFieldsAreIgnored() {
        server.respond(200, "{\"idUsuario\":\"" + USER + "\",\"generadoEn\":\"2026-10-06T09:00:00-03:00\","
                + "\"compromisos\":[],\"campoNuevo\":123}");
        assertThat(client.fetchPaymentFacts(USER).compromisos()).isEmpty();
    }

    // ---------- fallas de disponibilidad ----------
    @Test
    void serverErrorMapsToUnavailable() {
        server.respond(500, "{}");
        assertThatThrownBy(() -> client.fetchPaymentFacts(USER))
                .isInstanceOfSatisfying(CobranzaUnavailable.class, ex -> assertThat(ex.getStatus()).isEqualTo(503));
    }

    @Test
    void unauthorizedBadRequestAndForbiddenMapToUnavailable() {
        for (int status : new int[]{400, 401, 403, 429}) {
            server.respond(status, "{}");
            assertThatThrownBy(() -> client.fetchPaymentFacts(USER)).isInstanceOf(CobranzaUnavailable.class);
        }
    }

    @Test
    void connectionRefusedMapsToUnavailable() {
        server.stop();
        assertThatThrownBy(() -> client.fetchPaymentFacts(USER)).isInstanceOf(CobranzaUnavailable.class);
    }

    @Test
    void notFoundMapsToEmptyHistory() {
        server.respond(404, "{}");
        var facts = client.fetchPaymentFacts(USER);
        assertThat(facts.idUsuario()).isEqualTo(USER);
        assertThat(facts.compromisos()).isEmpty();
    }

    // ---------- fallas de datos (falla cerrada) ----------
    @Test
    void unexpectedSuccessStatusFailsClosed() {
        server.respond(204, "");
        assertThatThrownBy(() -> client.fetchPaymentFacts(USER))
                .isInstanceOfSatisfying(CobranzaInvalidData.class, ex -> assertThat(ex.getStatus()).isEqualTo(502));
    }

    @Test
    void malformedJsonFailsClosed() {
        server.respond(200, "{no es json");
        assertThatThrownBy(() -> client.fetchPaymentFacts(USER)).isInstanceOf(CobranzaInvalidData.class);
    }

    @Test
    void unknownStatusValueFailsClosed() {
        server.respond(200, "{\"idUsuario\":\"" + USER + "\",\"generadoEn\":\"2026-10-06T09:00:00-03:00\","
                + "\"compromisos\":[{\"idCompromiso\":\"" + UUID.randomUUID() + "\",\"folio\":\"X\","
                + "\"fechaVencimiento\":\"2026-07-05\",\"estado\":\"RARO\",\"pago\":null}]}");
        assertThatThrownBy(() -> client.fetchPaymentFacts(USER)).isInstanceOf(CobranzaInvalidData.class);
    }

    // --- helper ---
    private static String fixture() throws IOException {
        try (var stream = CobranzaClientTest.class.getResourceAsStream("/contracts/cobranza/hechos-pago.json")) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
