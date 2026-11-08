package com.pagoflex.usuarios.features.password.controller;

import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.dto.request.VerifyCurrentPasswordRequest;
import com.pagoflex.usuarios.features.password.dto.response.VerifyCurrentPasswordResponse;
import com.pagoflex.usuarios.features.password.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios/me/password")
@RequiredArgsConstructor
public class PasswordController {

    private final PasswordService service;

    @PostMapping("/verify")
    public ResponseEntity<VerifyCurrentPasswordResponse> verify(
            @Valid @RequestBody VerifyCurrentPasswordRequest request
    ) {
        return ResponseEntity.ok(service.verifyCurrentPassword(request));
    }

    @PutMapping
    public ResponseEntity<Void> change(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}