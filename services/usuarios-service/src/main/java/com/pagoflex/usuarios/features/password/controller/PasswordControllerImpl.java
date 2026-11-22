package com.pagoflex.usuarios.features.password.controller;

import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.service.PasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/usuarios/me/contrasena")
@RequiredArgsConstructor
public class PasswordControllerImpl implements PasswordController {
    private final PasswordService service;

    @Override
    public ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
