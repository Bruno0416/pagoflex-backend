package com.pagoflex.usuarios.features.recovery.controller;

import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResendRecoveryCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;
import com.pagoflex.usuarios.features.recovery.service.RecoveryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class RecoveryController {
    private final RecoveryService service;

    // 1. solicitar recuperación de contraseña
    @PostMapping("/recovery")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RecoveryRequest request) {
        service.requestPasswordReset(request);
        return ResponseEntity.ok().build();
    }

    // 2. reenviar código
    @PostMapping("/recovery/resend")
    public ResponseEntity<Void> resendCode(@Valid @RequestBody ResendRecoveryCodeRequest request) {
        service.resendCode(request);
        return ResponseEntity.ok().build();
    }

    // 3. verificar código (devuelve token de recuperación)
    @PostMapping("/recovery/verify")
    public ResponseEntity<VerifyResetTokenResponse> verifyResetCode(
            @Valid @RequestBody VerifyCodeRequest request) {
        return ResponseEntity.ok(service.verifyResetCode(request));
    }

    // 4. cambio de contraseña
    @PostMapping("/recovery/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request);
        return ResponseEntity.ok().build();
    }
}
