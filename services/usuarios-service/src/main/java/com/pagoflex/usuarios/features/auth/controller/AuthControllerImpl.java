package com.pagoflex.usuarios.features.auth.controller;

import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RefreshTokenRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthControllerImpl implements AuthController {
    private final AuthService service;

    @Override
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        service.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(service.login(request));
    }

    @Override
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(service.refresh(request.tokenRenovacion()));
    }

    @Override
    public ResponseEntity<Void> logout() {
        service.logout();
        return ResponseEntity.noContent().build();
    }
}
