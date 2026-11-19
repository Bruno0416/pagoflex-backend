package com.pagoflex.gateway;

import com.pagoflex.security.PublicPathsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityTest {
    @Autowired
    MockMvc mvc;

    @Autowired
    PublicPathsProperties publicPaths;

    @Test
    void internalRoutesAreNeverExposed() throws Exception {
        // 1. las rutas entre servicios no se publican a traves del gateway
        mvc.perform(post("/api/v1/usuarios/internal/personas")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/usuarios/internal/personas")
                        .with(jwt()).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void reconocimientosRoutesRequireJwt() throws Exception {
        // 1. las rutas publicas de reconocimientos no admiten peticiones anonimas
        for (String path : new String[]{"/api/v1/reconocimientos/me"}) {
            mvc.perform(get(path)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void genericInternalRoutesAreDeniedEvenWithJwt() throws Exception {
        // 1. ninguna ruta interna de otros servicios se expone, ni con identidad autenticada
        for (String path : new String[]{"/api/v1/internal/reconocimientos/evaluar"}) {
            mvc.perform(post(path).with(jwt()).contentType("application/json").content("{}"))
                    .andExpect(status().isForbidden());
            mvc.perform(post(path).contentType("application/json").content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void refreshRouteRemainsPublic() {
        // 1. comprobar la declaracion publica sin depender del servicio de usuarios
        assertThat(publicPaths.matchers()).contains("/api/v1/auth/renovar");
    }

    @Test
    void gatewayOwnHealthIsAvailable() throws Exception {
        // 1. comprobar el acceso anonimo aunque redis este inactivo
        mvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 503))
                .andExpect(jsonPath("$.status").exists());
    }
}
