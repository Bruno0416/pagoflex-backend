package com.pagoflex.usuarios.core.security;

import com.pagoflex.usuarios.features.auth.entity.Users;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtil jwtUtil;

    // ---------- rutas publicas ----------
    @Test
    void pathsDeclaredInYamlRemainPublic() throws Exception {
        // 1. comprobar que la validacion del dto ocurre sin un jwt
        mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthRemainsPublic() throws Exception {
        // 1. permitir health sin jwt aunque alguna dependencia esté desconectada
        mvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isIn(200, 503));
    }

    // ---------- rutas privadas ----------
    @Test
    void unlistedEndpointRequiresAuthentication() throws Exception {
        // 1. rechazar peticiones sin jwt antes de validar el dto
        mvc.perform(post("/api/v1/usuarios/me/password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtCannotAccessPrivateEndpoint() throws Exception {
        mvc.perform(post("/api/v1/usuarios/me/password/verify")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validAccessJwtReachesPrivateController() throws Exception {
        // 1. emitir un token real con el jwtutil del servicio
        Users user = Users.builder()
                .id(UUID.randomUUID())
                .email("prueba@example.com")
                .role(Role.USUARIO_FINAL)
                .build();
        String token = jwtUtil.generateToken(user);

        // 2. comprobar que el endpoint aplica su validacion tras autenticar
        mvc.perform(post("/api/v1/usuarios/me/password/verify")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
