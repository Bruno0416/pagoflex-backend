package com.pagoflex.usuarios.features.profile.controller;

import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.dto.response.ProfileResponse;
import com.pagoflex.usuarios.features.profile.dto.response.UpdateProfileResponse;
import com.pagoflex.usuarios.features.profile.service.ProfileService;
import com.pagoflex.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class ProfileControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean ProfileService service;

    private ProfileResponse response() {
        return new ProfileResponse(UUID.randomUUID(), UUID.randomUUID(), "99999999-9", "Usuario", "Prueba",
                "usuario@example.com", null, "USUARIO_FINAL", null, null);
    }

    // ---------- perfil ----------
    @Test
    void getProfileReturnsUserData() throws Exception {
        when(service.getCurrentProfile()).thenReturn(response());
        mvc.perform(get("/api/v1/usuarios/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("usuario@example.com"))
                .andExpect(jsonPath("$.idPersona").exists())
                .andExpect(jsonPath("$.rol").value("USUARIO_FINAL"));
    }

    @Test
    void patchProfileReturnsUpdatedData() throws Exception {
        when(service.updateProfile(any())).thenReturn(new UpdateProfileResponse(response(), null));
        mvc.perform(patch("/api/v1/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("""
                {"nombres":"Usuario","apellidos":"Prueba"}
                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil.nombres").value("Usuario"))
                .andExpect(jsonPath("$.perfil.correo").value("usuario@example.com"));
    }

    @Test
    void patchBindsSpanishEmailAndPasswordFields() throws Exception {
        when(service.updateProfile(any())).thenReturn(new UpdateProfileResponse(response(), null));
        mvc.perform(patch("/api/v1/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"nuevo@example.com","contrasenaActual":"ClaveActual123!"}
                """))
                .andExpect(status().isOk());
        verify(service).updateProfile(new UpdateProfileRequest(
                null, null, "nuevo@example.com", "ClaveActual123!"));
    }

    @Test
    void patchRejectsInvalidEmailBeforeReachingService() throws Exception {
        mvc.perform(patch("/api/v1/usuarios/me").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"correo-invalido"}
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.correo").exists());
        verifyNoInteractions(service);
    }

    // ---------- avatar ----------
    @Test
    void getAvatarReturnsJpeg() throws Exception {
        when(service.getAvatar()).thenReturn(new byte[]{1, 2, 3});
        mvc.perform(get("/api/v1/usuarios/me/avatar"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"));
    }

    @Test
    void uploadAvatarAcceptsMultipart() throws Exception {
        when(service.uploadAvatar(any())).thenReturn(response());
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});
        MockMultipartHttpServletRequestBuilder request = multipart("/api/v1/usuarios/me/avatar");
        request.with(req -> { req.setMethod("PUT"); return req; });
        mvc.perform(request.file(file)).andExpect(status().isOk());
        verify(service).uploadAvatar(any());
    }

    @Test
    void deleteAvatarReturnsNoContent() throws Exception {
        mvc.perform(delete("/api/v1/usuarios/me/avatar")).andExpect(status().isNoContent());
        verify(service).deleteAvatar();
    }
}
