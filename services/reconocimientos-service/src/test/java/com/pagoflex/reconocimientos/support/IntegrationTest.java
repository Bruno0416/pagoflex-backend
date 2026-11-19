package com.pagoflex.reconocimientos.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestClockConfig.class)
public abstract class IntegrationTest {
    protected static final String DEFAULT_NOW = "2026-10-06T12:00:00Z";
    protected static final String UF = "USUARIO_FINAL";
    protected static final String AG = "AGENTE";
    protected static final String EJ = "EJECUTIVO";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected MutableClock clock;

    @MockitoBean
    protected StringRedisTemplate redis;

    protected ValueOperations<String, String> ops;
    protected final CobranzaStubServer cobranza = CobranzaStubServer.shared();

    @DynamicPropertySource
    static void cobranzaUrl(DynamicPropertyRegistry registry) {
        registry.add("pagoflex.reconocimientos.cobranza.base-url", () -> CobranzaStubServer.shared().baseUrl());
    }

    @BeforeEach
    void resetCollaborators() {
        // 1. redis simulado, cobranza limpia y reloj en su instante por defecto
        ops = mock();
        when(redis.opsForValue()).thenReturn(ops);
        cobranza.reset();
        clock.set(DEFAULT_NOW);
    }

    // ---------- tokens ----------
    protected String token(UUID userId, String role) {
        return "Bearer " + TestJwtFactory.access(ops, userId, role);
    }
}
