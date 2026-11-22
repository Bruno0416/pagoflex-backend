package com.pagoflex.usuarios.features.internal.controller;

import com.pagoflex.contracts.usuarios.SolicitudCrearPersonaInterna;
import com.pagoflex.contracts.usuarios.RespuestaPersonaInterna;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Internal Person")
public interface InternalPersonController {

    @Operation(summary = "Registrar persona")
    @ApiResponse(responseCode = "201", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @PostMapping
    ResponseEntity<RespuestaPersonaInterna> create(@Valid @RequestBody SolicitudCrearPersonaInterna request);

    @Operation(summary = "Consultar persona por RUT")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @GetMapping("/por-rut/{rut}")
    ResponseEntity<RespuestaPersonaInterna> byRut(@PathVariable String rut);

    @Operation(summary = "Consultar persona por usuario")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @GetMapping("/por-usuario/{idUsuario}")
    ResponseEntity<RespuestaPersonaInterna> byUserId(@PathVariable UUID idUsuario);

    @Operation(summary = "Consultar persona por correo")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @GetMapping("/por-correo/{correo}")
    ResponseEntity<RespuestaPersonaInterna> byEmail(@PathVariable String correo);

    @Operation(summary = "Consultar persona por ID")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @GetMapping("/{id}")
    ResponseEntity<RespuestaPersonaInterna> byId(@PathVariable UUID id);

}
