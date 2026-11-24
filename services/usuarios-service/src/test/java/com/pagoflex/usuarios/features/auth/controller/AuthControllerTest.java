package com.pagoflex.usuarios.features.auth.controller;

import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidCredentials;
import com.pagoflex.usuarios.features.auth.service.AuthService;
import com.pagoflex.web.error.GlobalExceptionHandler;
import com.pagoflex.usuarios.core.security.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthControllerImpl.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AuthService service;

    // ---------- registro ----------
    @Test
    void registerReturns201() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"Clave123!","rut":"99999999-9","nombres":"Usuario","apellidos":"Prueba"}
                """)).andExpect(status().isCreated());
        verify(service).register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null));
    }

    @Test
    void registerRejectsInvalidFields() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"invalido","contrasena":"123"}
                """)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        doThrow(new EmailAlreadyInUse("usuario@example.com")).when(service).register(any(RegisterRequest.class));

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"Clave123!","rut":"99999999-9","nombres":"Usuario","apellidos":"Prueba"}
                """)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.invalid_fields.correo").exists());
    }

    @Test
    void registerReturns409ForDuplicateRut() throws Exception {
        doThrow(new RutAlreadyInUse()).when(service).register(any(RegisterRequest.class));

        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"Clave123!","rut":"99999999-9","nombres":"Usuario","apellidos":"Prueba"}
                """)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.invalid_fields.rut").exists());
    }

    @Test
    void registerRejectsRutWithDots() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"correo":"usuario@example.com","contrasena":"Clave123!","rut":"99.999.999-9","nombres":"Usuario","apellidos":"Prueba"}
                        """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.rut").exists());
        verifyNoInteractions(service);
    }

    @Test
    void registerRejectsInvalidCompanyCodeWithFieldError() throws Exception {
        // 1. validar el contrato antes de crear cualquier cuenta
        for (String codigo : new String[]{"CODIGO-AGENTE", "AB12A", "ab12ab", "A112AA", "AB12CD-ABCDEFGHIJKLMNOPQRSTUVWXYZ"}) {
            mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":\"usuario@example.com\",\"contrasena\":\"Clave123!\","
                                    + "\"rut\":\"99999999-9\",\"nombres\":\"Usuario\","
                                    + "\"apellidos\":\"Prueba\",\"codigoEmpresa\":\"" + codigo + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.invalid_fields.codigoEmpresa").exists());
        }
        verifyNoInteractions(service);
    }

    @Test
    void registerAcceptsSecureCompanyCode() throws Exception {
        mvc.perform(post("/api/v1/auth/registro").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"Clave123!","rut":"99999999-9",
                 "nombres":"Usuario","apellidos":"Prueba","codigoEmpresa":"PF12AG"}
                """)).andExpect(status().isCreated());
        verify(service).register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9",
                "Usuario", "Prueba", "PF12AG"));
    }

    // ---------- inicio de sesion ----------
    @Test
    void loginReturnsJwt() throws Exception {
        when(service.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-de-prueba", "refresh-prueba", Role.USUARIO_FINAL));

        mvc.perform(post("/api/v1/auth/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"contrasena123"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.tokenAcceso").value("jwt-de-prueba"))
                .andExpect(jsonPath("$.tokenRenovacion").value("refresh-prueba"))
                .andExpect(jsonPath("$.rol").value("USUARIO_FINAL"))
                .andExpect(jsonPath("$.tipoToken").doesNotExist())
                .andExpect(jsonPath("$.expiraEnSegundos").doesNotExist());
    }

    @Test
    void loginRejectsEmptyPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":""}
                """)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void loginRejectsIncorrectCredentials() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new InvalidCredentials());

        mvc.perform(post("/api/v1/auth/iniciar-sesion").contentType(MediaType.APPLICATION_JSON).content("""
                {"correo":"usuario@example.com","contrasena":"incorrecta"}
                """)).andExpect(status().isUnauthorized());
    }

    // ---------- renovar y cerrar sesion ----------
    @Test
    void refreshReturnsNewTokenPair() throws Exception {
        when(service.refresh("refresh-anterior"))
                .thenReturn(new AuthResponse("access-nuevo", "refresh-nuevo", Role.USUARIO_FINAL));
        mvc.perform(post("/api/v1/auth/renovar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRenovacion\":\"refresh-anterior\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("access-nuevo"))
                .andExpect(jsonPath("$.tokenRenovacion").value("refresh-nuevo"))
                .andExpect(jsonPath("$.rol").value("USUARIO_FINAL"))
                .andExpect(jsonPath("$.tipoToken").doesNotExist())
                .andExpect(jsonPath("$.expiraEnSegundos").doesNotExist());
    }

    @Test
    void refreshRejectsEmptyToken() throws Exception {
        mvc.perform(post("/api/v1/auth/renovar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tokenRenovacion\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutReturns204() throws Exception {
        mvc.perform(post("/api/v1/auth/cerrar-sesion")).andExpect(status().isNoContent());
        verify(service).logout();
    }

}
