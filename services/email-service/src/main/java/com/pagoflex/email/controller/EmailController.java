package com.pagoflex.email.controller;

import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.email.service.EmailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/email/internal")
@RequiredArgsConstructor
public class EmailController {
    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<Void> send(@Valid @RequestBody SendEmailRequest request) {
        emailService.send(request);
        return ResponseEntity.ok().build();
    }
}
