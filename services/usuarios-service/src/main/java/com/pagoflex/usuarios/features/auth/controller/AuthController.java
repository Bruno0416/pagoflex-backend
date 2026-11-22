package com.pagoflex.usuarios.features.auth.controller;

import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RefreshTokenRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Auth")
public interface AuthController {

    @Operation(summary = "Registrar una cuenta")
    @ApiResponse(responseCode = "201", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/registro")
    ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request);

    @Operation(summary = "Iniciar sesión")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/iniciar-sesion")
    ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request);

    @Operation(summary = "Renovar sesión")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/renovar")
    ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request);

    @Operation(summary = "Cerrar sesión")
    @ApiResponse(responseCode = "204", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/cerrar-sesion")
    ResponseEntity<Void> logout();

}
