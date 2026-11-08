package com.pagoflex.contracts.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record SendEmailRequest(
        @Email @NotBlank String to,
        @NotNull EmailType type,
        @NotNull Map<String, @NotBlank String> variables
) {
}
