package com.pagoflex.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GatewaySecurityTest {
    @Autowired
    MockMvc mvc;

    @Test
    void internalAndRemovedRoutesAreNotPublic() throws Exception {
        // 1. comprobar que los endpoints internos y retirados no son públicos
        for (String path : new String[]{
                "/api/v1/content", "/api/v1/search", "/api/v1/users/me",
                "/api/v1/email/internal/send", "/api/v1/auth/refresh",
                "/api/v1/auth/confirm-email"}) {
            mvc.perform(post(path).contentType("application/json").content("{}"))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Test
    void gatewayOwnHealthIsAvailable() throws Exception {
        // 1. comprobar que el health del gateway es accesible
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
