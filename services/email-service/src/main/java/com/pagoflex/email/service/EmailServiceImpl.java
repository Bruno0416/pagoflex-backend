package com.pagoflex.email.service;

import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.core.error.PagoFlexException;
import com.pagoflex.email.theme.EmailTheme;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {
    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Override
    public void send(SendEmailRequest request) {
        // 1. validar las variables que necesita el tipo de correo
        for (String variable : request.type().requiredVariables()) {
            String value = request.variables().get(variable);
            if (value == null || value.isBlank()) {
                throw new PagoFlexException(HttpStatus.BAD_REQUEST.value(),
                        "Falta la variable requerida: " + variable);
            }
        }

        // 2. procesar la plantilla seleccionada por el enum
        Context context = new Context(Locale.of("es"));
        request.variables().forEach(context::setVariable);
        context.setVariable("theme", EmailTheme.forTemplate(request.type().templateName()));
        String html = templateEngine.process(request.type().templateName(), context);

        // 3. construir y enviar el correo por smtp
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress, "PagoFlex");
            helper.setTo(request.to());
            helper.setSubject(request.type().subject());
            helper.setText(html, true);
            mailSender.send(message);
            log.info("Correo de tipo {} enviado correctamente", request.type());
        } catch (MessagingException | MailException | java.io.UnsupportedEncodingException ex) {
            // 4. informar el fallo sin exponer datos de smtp
            log.error("No fue posible enviar el correo de tipo {}", request.type(), ex);
            throw new PagoFlexException(HttpStatus.BAD_GATEWAY.value(),
                    "No fue posible enviar el correo. Inténtalo nuevamente.");
        }
    }
}
