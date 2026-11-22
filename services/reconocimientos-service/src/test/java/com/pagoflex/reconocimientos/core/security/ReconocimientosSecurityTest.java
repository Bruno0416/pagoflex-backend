package com.pagoflex.reconocimientos.core.security;

import com.pagoflex.reconocimientos.support.IntegrationTest;
import com.pagoflex.reconocimientos.support.TestJwtFactory;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReconocimientosSecurityTest extends IntegrationTest {
    // ruta visible para el usuario final
    private static final List<String[]> PUBLIC_API = List.<String[]>of(
            new String[]{"GET", "/api/v1/reconocimientos/me"});

    private final UUID userId = UUID.randomUUID();

    // ---------- autenticacion ----------
    @Test
    void everyPublicRouteRequiresJwt() throws Exception {
        for (String[] route : PUBLIC_API) {
            mvc.perform(call(route, null)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void expiredJwt401() throws Exception {
        String token = TestJwtFactory.expired(ops, userId, UF);
        mvc.perform(call(new String[]{"GET", "/api/v1/reconocimientos/me"}, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenType401() throws Exception {
        String token = TestJwtFactory.refreshType(ops, userId, UF);
        mvc.perform(call(new String[]{"GET", "/api/v1/reconocimientos/me"}, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedSessionInRedis401() throws Exception {
        // 1. token firmado pero sin sesion vigente en redis
        String token = TestJwtFactory.withoutSession(ops, userId, UF);
        mvc.perform(call(new String[]{"GET", "/api/v1/reconocimientos/me"}, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void redisDown401() throws Exception {
        // 1. un token valido se rechaza si redis no responde
        String token = TestJwtFactory.access(ops, userId, UF);
        when(redis.opsForValue()).thenThrow(new IllegalStateException("redis caido"));
        mvc.perform(call(new String[]{"GET", "/api/v1/reconocimientos/me"}, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedBearer401() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/api/v1/reconocimientos/me")
                        .header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- autorizacion por rol ----------
    @Test
    void executiveForbiddenEverywhere() throws Exception {
        // 1. rn-22: el ejecutivo no accede a nada de reconocimientos
        String token = TestJwtFactory.access(ops, userId, EJ);
        for (String[] route : PUBLIC_API) {
            mvc.perform(call(route, token)).andExpect(status().isForbidden());
        }
    }

    @Test
    void agentCannotReadOwnRecognitions() throws Exception {
        String token = TestJwtFactory.access(ops, userId, AG);
        mvc.perform(call(PUBLIC_API.get(0), token)).andExpect(status().isForbidden());
    }

    @Test
    void undeclaredRoutesAreDenied() throws Exception {
        String token = TestJwtFactory.access(ops, userId, UF);
        mvc.perform(call(new String[]{"DELETE", "/api/v1/reconocimientos/me"}, token))
                .andExpect(status().isForbidden());
    }

    // ---------- rutas internas ----------
    @Test
    void internalRouteIgnoresJwtWithoutKey() throws Exception {
        String token = TestJwtFactory.access(ops, userId, AG);
        mvc.perform(call(new String[]{"POST", "/api/v1/internal/reconocimientos/evaluar"}, token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalRouteRejectsMissingOrWrongKey() throws Exception {
        mvc.perform(request(HttpMethod.POST, "/api/v1/internal/reconocimientos/evaluar")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(request(HttpMethod.POST, "/api/v1/internal/reconocimientos/evaluar")
                        .header("X-Internal-Api-Key", "otra-clave")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- publico y errores de enrutamiento ----------
    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(request(HttpMethod.GET, "/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 503));
    }

    // --- helper ---
    private MockHttpServletRequestBuilder call(String[] route, String token) {
        // 1. armar la peticion con cuerpo vacio y el token opcional
        MockHttpServletRequestBuilder builder = request(HttpMethod.valueOf(route[0]), route[1])
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return builder;
    }
}
