package com.pagoflex.usuarios.features.recovery.controller;

import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResendRecoveryCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidRecoveryCode;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import com.pagoflex.usuarios.features.recovery.service.RecoveryService;
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

@WebMvcTest(RecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class RecoveryControllerTest {
    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RecoveryService service;

    // ---------- recuperación de contraseña ----------
    @Test
    void recoveryReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@example.com"}
                                """))
                .andExpect(status().isOk());
        verify(service).requestPasswordReset(new RecoveryRequest("usuario@example.com"));
    }

    @Test
    void recoveryRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/recovery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-es-un-correo"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void verifyRecoveryReturnsTemporaryToken() throws Exception {
        when(service.verifyResetCode(any(VerifyCodeRequest.class)))
                .thenReturn(new VerifyResetTokenResponse("token-temporal"));

        mvc.perform(post("/api/v1/auth/recovery/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@example.com","code":"ABC123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("token-temporal"));
    }

    @Test
    void verifyRecoveryRejectsInvalidCode() throws Exception {
        when(service.verifyResetCode(any(VerifyCodeRequest.class)))
                .thenThrow(new InvalidRecoveryCode("El código es inválido o ha expirado"));

        mvc.perform(post("/api/v1/auth/recovery/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@example.com","code":"ABC123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendRecoveryReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/recovery/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"usuario@example.com"}
                                """))
                .andExpect(status().isOk());
        verify(service).resendCode(new ResendRecoveryCodeRequest("usuario@example.com"));
    }

    @Test
    void resetPasswordReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resetToken":"token-temporal","newPassword":"nuevaClave123"}
                                """))
                .andExpect(status().isOk());
        verify(service).resetPassword(new ResetPasswordRequest("token-temporal", "nuevaClave123"));
    }

    @Test
    void resetPasswordRejectsExpiredToken() throws Exception {
        doThrow(new InvalidResetToken()).when(service).resetPassword(any(ResetPasswordRequest.class));

        mvc.perform(post("/api/v1/auth/recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resetToken":"token-expirado","newPassword":"nuevaClave123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
