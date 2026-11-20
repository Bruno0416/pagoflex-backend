package com.pagoflex.usuarios.core.security;

import com.pagoflex.usuarios.features.auth.entity.Users;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {
    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtUtil jwtUtil;

    @MockitoBean
    private StringRedisTemplate redis;

    // ---------- rutas publicas ----------
    @Test
    void pathsDeclaredInYamlRemainPublic() throws Exception {
        // 1. comprobar que la validacion del dto ocurre sin un jwt
        mvc.perform(post("/api/v1/auth/iniciar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void healthRemainsPublic() throws Exception {
        // 1. permitir health sin jwt aunque alguna dependencia este desconectada
        mvc.perform(get("/actuator/health"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .isIn(200, 503));
    }

    // ---------- rutas privadas ----------
    @Test
    void unlistedEndpointRequiresAuthentication() throws Exception {
        // 1. rechazar peticiones sin jwt antes de validar el dto
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileEndpointsArePrivateByDefault() throws Exception {
        // 1. comprobar que el perfil y el avatar no son publicos
        mvc.perform(get("/api/v1/usuarios/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/usuarios/me/avatar")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidJwtCannotAccessPrivateEndpoint() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
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
        UUID sid = UUID.randomUUID();
        UUID jti = UUID.randomUUID();
        ValueOperations<String, String> values = mock();
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("auth:access:" + jti)).thenReturn(sid.toString());
        when(values.get("auth:session:" + sid))
                .thenReturn(user.getId() + "|" + jti + "|hash");
        String token = jwtUtil.generateToken(user, sid, jti);

        // 2. comprobar que el endpoint aplica su validacion tras autenticar
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
    // ---------- proteccion de endpoints internos ----------
    @Test
    void internalPersonRouteRequiresServiceKey() throws Exception {
        mvc.perform(post("/api/v1/usuarios/internal/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validServiceKeyCanReachInternalValidation() throws Exception {
        mvc.perform(post("/api/v1/usuarios/internal/personas")
                        .header("X-Internal-Api-Key", "test-only-internal-api-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshIsPublicButRequiresValidBody() throws Exception {
        mvc.perform(post("/api/v1/auth/renovar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutAlwaysRequiresAccessToken() throws Exception {
        mvc.perform(post("/api/v1/auth/cerrar-sesion"))
                .andExpect(status().isUnauthorized());
    }

}
