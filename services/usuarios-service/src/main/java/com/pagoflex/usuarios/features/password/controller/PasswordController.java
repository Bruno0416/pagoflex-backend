package com.pagoflex.usuarios.features.password.controller;

import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Password")
public interface PasswordController {

    @Operation(summary = "Cambiar contraseña")
    @ApiResponse(responseCode = "204", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping
    ResponseEntity<Void> change(@Valid @RequestBody ChangePasswordRequest request);

}
