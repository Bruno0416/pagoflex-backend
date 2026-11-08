package com.pagoflex.usuarios.features.auth.controller;

import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidCredentials;
import com.pagoflex.usuarios.features.auth.service.AuthService;
import com.pagoflex.web.error.GlobalExceptionHandler;
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

@WebMvcTest(AuthController.class)
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
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"Clave123!","rut":"99999999-9","nombres":"Usuario","apellidos":"Prueba"}
                """)).andExpect(status().isCreated());
        verify(service).register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba"));
    }

    @Test
    void registerRejectsInvalidFields() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"invalido","password":"123"}
                """)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        doThrow(new EmailAlreadyInUse("usuario@example.com")).when(service).register(any(RegisterRequest.class));

        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"Clave123!","rut":"99999999-9","nombres":"Usuario","apellidos":"Prueba"}
                """)).andExpect(status().isConflict());
    }

    @Test
    void registerRejectsRutWithDots() throws Exception {
        mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"email":"usuario@example.com","password":"Clave123!","rut":"99.999.999-9","nombres":"Usuario","apellidos":"Prueba"}
                        """)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalid_fields.rut").exists());
        verifyNoInteractions(service);
    }

    // ---------- inicio de sesión ----------
    @Test
    void loginReturnsJwt() throws Exception {
        when(service.login(any(LoginRequest.class))).thenReturn(new AuthResponse("jwt-de-prueba"));

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"contrasena123"}
                """)).andExpect(status().isOk()).andExpect(jsonPath("$.token").value("jwt-de-prueba"));
    }

    @Test
    void loginRejectsEmptyPassword() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":""}
                """)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void loginRejectsIncorrectCredentials() throws Exception {
        when(service.login(any(LoginRequest.class))).thenThrow(new InvalidCredentials());

        mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"usuario@example.com","password":"incorrecta"}
                """)).andExpect(status().isUnauthorized());
    }

}
