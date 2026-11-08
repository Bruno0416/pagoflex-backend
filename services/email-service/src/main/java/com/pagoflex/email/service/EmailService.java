package com.pagoflex.email.service;

import com.pagoflex.contracts.email.SendEmailRequest;

public interface EmailService {
    void send(SendEmailRequest request);
}
