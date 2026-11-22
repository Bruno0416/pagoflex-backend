package com.pagoflex.usuarios.features.password.controller;

import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.service.PasswordService;
import com.pagoflex.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordControllerImpl.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PasswordControllerTest {
    @Autowired private MockMvc mvc;
    @MockitoBean private PasswordService service;

    // ---------- cambio directo desde perfil ----------
    @Test
    void changeAcceptsCurrentAndNewPassword() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual":"ClaveActual123!","nuevaContrasena":"NuevaClave123!","confirmacionContrasena":"NuevaClave123!"}
                                """))
                .andExpect(status().isNoContent());
        verify(service).changePassword(new ChangePasswordRequest("ClaveActual123!", "NuevaClave123!", "NuevaClave123!"));
    }

    @Test
    void changeRejectsMissingCurrentPassword() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuevaContrasena\":\"NuevaClave123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.contrasenaActual").exists());
        verifyNoInteractions(service);
    }

    @Test
    void changeRejectsMissingConfirmation() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"ClaveActual123!\",\"nuevaContrasena\":\"NuevaClave123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.confirmacionContrasena").exists());
        verifyNoInteractions(service);
    }

    @Test
    void changeRejectsWeakPassword() throws Exception {
        mvc.perform(put("/api/v1/usuarios/me/contrasena")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contrasenaActual\":\"ClaveActual123!\",\"nuevaContrasena\":\"corta\",\"confirmacionContrasena\":\"corta\"}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
