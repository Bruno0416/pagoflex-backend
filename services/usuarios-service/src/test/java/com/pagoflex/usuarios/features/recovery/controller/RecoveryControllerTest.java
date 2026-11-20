package com.pagoflex.usuarios.features.recovery.controller;

import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
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
        mvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"usuario@example.com"}
                                """))
                .andExpect(status().isOk());
        verify(service).requestPasswordReset(new RecoveryRequest("usuario@example.com"));
    }

    @Test
    void recoveryRejectsInvalidEmail() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"no-es-un-correo"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void verifyRecoveryReturnsTemporaryToken() throws Exception {
        when(service.verifyResetCode(any(VerifyCodeRequest.class)))
                .thenReturn(new VerifyResetTokenResponse("token-temporal"));

        mvc.perform(post("/api/v1/auth/recuperacion/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"usuario@example.com","codigo":"ABC123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenRestablecimiento").value("token-temporal"));
    }

    @Test
    void verifyRecoveryRejectsInvalidCode() throws Exception {
        when(service.verifyResetCode(any(VerifyCodeRequest.class)))
                .thenThrow(new InvalidRecoveryCode("El código es inválido o ha expirado"));

        mvc.perform(post("/api/v1/auth/recuperacion/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"usuario@example.com","codigo":"ABC123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resendRecoveryReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperacion/reenviar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"usuario@example.com"}
                                """))
                .andExpect(status().isOk());
        verify(service).resendCode(new RecoveryRequest("usuario@example.com"));
    }

    @Test
    void resetPasswordReturns200() throws Exception {
        mvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRestablecimiento":"token-temporal","nuevaContrasena":"NuevaClave123!"}
                                """))
                .andExpect(status().isOk());
        verify(service).resetPassword(new ResetPasswordRequest("token-temporal", "NuevaClave123!"));
    }

    @Test
    void resetPasswordRequiresAllFivePasswordRules() throws Exception {
        // Largo, mayuscula, minuscula, numero y caracter especial; se valida antes de consumir token.
        for (String password : new String[]{"Aa1!", "minuscula123!", "MAYUSCULA123!",
                "SinNumeros!", "SinEspecial123"}) {
            mvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"tokenRestablecimiento\":\"token-temporal\",\"nuevaContrasena\":\""
                                    + password + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.invalid_fields.nuevaContrasena").exists());
        }
        verifyNoInteractions(service);
    }

    @Test
    void resetPasswordRejectsExpiredToken() throws Exception {
        doThrow(new InvalidResetToken()).when(service).resetPassword(any(ResetPasswordRequest.class));

        mvc.perform(post("/api/v1/auth/recuperacion/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRestablecimiento":"token-expirado","nuevaContrasena":"NuevaClave123!"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
