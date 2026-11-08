package com.pagoflex.email.controller;

import com.pagoflex.contracts.email.EmailType;
import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.core.error.PagoFlexException;
import com.pagoflex.email.config.SecurityConfig;
import com.pagoflex.email.service.EmailService;
import com.pagoflex.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, GlobalExceptionHandler.class, EmailControllerTest.TestSecurityConfiguration.class})
class EmailControllerTest {
    @TestConfiguration
    @EnableWebSecurity
    static class TestSecurityConfiguration {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EmailService service;

    private static final String ENDPOINT = "/api/v1/email/internal/send";
    private static final String INTERNAL_KEY = "test-only-internal-api-key";
    private static final String VALID_REQUEST = """
            {"to":"usuario@example.com","type":"RECOVERY","variables":{"code":"ABC123"}}
            """;

    // ---------- autorizacion interna ----------
    @Test
    void rejectsRequestsWithoutInternalKey() throws Exception {
        mvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsIncorrectInternalKey() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", "clave-incorrecta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }

    // ---------- envio de correos ----------
    @Test
    void sendsRecoveryEmailWhenKeyAndBodyAreValid() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());
        verify(service).send(new SendEmailRequest("usuario@example.com", EmailType.RECOVERY,
                Map.of("code", "ABC123")));
    }

    @Test
    void rejectsInvalidRecipient() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"correo-invalido","type":"RECOVERY","variables":{"code":"ABC123"}}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingEmailType() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"usuario@example.com","variables":{"code":"ABC123"}}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsUnsupportedEmailType() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"usuario@example.com","type":"UNKNOWN","variables":{"code":"ABC123"}}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void rejectsMissingVariables() throws Exception {
        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to":"usuario@example.com","type":"RECOVERY"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void propagatesMailDeliveryFailuresAs502() throws Exception {
        doThrow(new PagoFlexException(HttpStatus.BAD_GATEWAY.value(), "No fue posible enviar el correo"))
                .when(service).send(any(SendEmailRequest.class));

        mvc.perform(post(ENDPOINT)
                        .header("X-Internal-Api-Key", INTERNAL_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST))
                .andExpect(status().isBadGateway());
    }
}
