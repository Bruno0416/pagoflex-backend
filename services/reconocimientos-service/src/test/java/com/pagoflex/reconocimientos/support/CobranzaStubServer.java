package com.pagoflex.reconocimientos.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public final class CobranzaStubServer {
    private static CobranzaStubServer shared;

    public volatile int status = 200;
    public volatile String body = "{}";
    public volatile String receivedKey;
    public volatile String receivedPath;
    public final AtomicInteger calls = new AtomicInteger();

    private final HttpServer server;

    public CobranzaStubServer() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
        server.createContext("/", exchange -> {
            calls.incrementAndGet();
            receivedKey = exchange.getRequestHeaders().getFirst("X-Internal-Api-Key");
            receivedPath = exchange.getRequestURI().getPath();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            if (status == 204) {
                exchange.sendResponseHeaders(status, -1);
            } else {
                exchange.sendResponseHeaders(status, bytes.length);
                try (var stream = exchange.getResponseBody()) {
                    stream.write(bytes);
                }
            }
            exchange.close();
        });
        server.start();
    }

    // ---------- instancia compartida entre contextos de spring ----------
    public static synchronized CobranzaStubServer shared() {
        if (shared == null) shared = new CobranzaStubServer();
        return shared;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/internal";
    }

    public void respond(int status, String body) {
        this.status = status;
        this.body = body;
    }

    public void reset() {
        status = 200;
        body = "{}";
        receivedKey = null;
        receivedPath = null;
        calls.set(0);
    }

    public void stop() {
        server.stop(0);
    }
}
