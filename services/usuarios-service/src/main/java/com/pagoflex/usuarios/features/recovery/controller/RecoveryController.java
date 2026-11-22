package com.pagoflex.usuarios.features.recovery.controller;

import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Recovery")
public interface RecoveryController {

    @Operation(summary = "Solicitar recuperación")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/recuperacion")
    ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody RecoveryRequest request);

    @Operation(summary = "Reenviar código")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/recuperacion/reenviar")
    ResponseEntity<Void> resendCode(@Valid @RequestBody RecoveryRequest request);

    @Operation(summary = "Verificar código")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/recuperacion/verificar")
    ResponseEntity<VerifyResetTokenResponse> verifyResetCode(@Valid @RequestBody VerifyCodeRequest request);

    @Operation(summary = "Restablecer contraseña")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @PostMapping("/recuperacion/restablecer")
    ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request);

}
