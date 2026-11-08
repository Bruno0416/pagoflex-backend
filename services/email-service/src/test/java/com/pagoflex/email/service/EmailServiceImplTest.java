package com.pagoflex.email.service;

import com.pagoflex.contracts.email.EmailType;
import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.core.error.PagoFlexException;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {
    @Mock
    private JavaMailSender mailSender;
    private EmailServiceImpl service;

    private static final SendEmailRequest RECOVERY_REQUEST = new SendEmailRequest(
            "usuario@example.com", EmailType.RECOVERY, Map.of("code", "ABC123"));

    @BeforeEach
    void setup() {
        // 1. usar la plantilla real para validar el renderizado
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);
        service = new EmailServiceImpl(mailSender, templateEngine);
        ReflectionTestUtils.setField(service, "fromAddress", "remitente@example.com");
    }

    // ---------- plantilla y envio smtp ----------
    @Test
    void rendersHtmlAndSendsEmailToRecipient() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        service.send(RECOVERY_REQUEST);

        assertThat(message.getContent().toString()).contains("ABC123");
        verify(mailSender).send(message);
        assertThat(message.getSubject()).isEqualTo("Recupera tu contraseña de PagoFlex");
        assertThat(message.getRecipients(Message.RecipientType.TO)[0].toString())
                .isEqualTo("usuario@example.com");
        // el mock no finaliza los encabezados MIME como el sender real
        message.saveChanges();
        assertThat(message.getContentType()).contains("text/html");
    }

    @Test
    void rejectsMissingRequiredVariableWithoutSending() {
        SendEmailRequest request = new SendEmailRequest("usuario@example.com", EmailType.RECOVERY, Map.of());

        assertThatThrownBy(() -> service.send(request))
                .isInstanceOf(PagoFlexException.class)
                .satisfies(error -> assertThat(((PagoFlexException) error).getStatus()).isEqualTo(400));
        verifyNoInteractions(mailSender);
    }

    @Test
    void smtpFailureReturnsBadGatewayWithoutExposingCredentials() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("error smtp interno"))
                .when(mailSender).send(message);

        assertThatThrownBy(() -> service.send(RECOVERY_REQUEST))
                .isInstanceOf(PagoFlexException.class)
                .hasMessageContaining("No fue posible enviar el correo")
                .satisfies(error -> assertThat(((PagoFlexException) error).getStatus()).isEqualTo(502));
    }
}
