package com.pagoflex.usuarios.core.client;

import com.pagoflex.contracts.email.SendEmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component
public class EmailServiceClient {
    private final WebClient client;

    // ---------- comunicación interna ----------
    public EmailServiceClient(
            @Value("${email-service.api.base-url}") String baseUrl,
            @Value("${pagoflex.security.internal-api-key}") String internalKey) {
        // 1. configurar el cliente http y su clave interna
        this.client = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("X-Internal-Api-Key", internalKey)
                .build();
    }

    public void send(SendEmailRequest request) {
        // 1. solicitar el envío al servicio interno
        client.post()
                .uri("/send")
                .bodyValue(request)
                .retrieve()
                .toBodilessEntity()
                .block(Duration.ofSeconds(10));
    }
}
