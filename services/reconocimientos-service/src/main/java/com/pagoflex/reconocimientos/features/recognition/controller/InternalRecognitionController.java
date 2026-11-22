package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.contracts.vinculacion.SolicitudEvaluacionReconocimientos;
import com.pagoflex.contracts.vinculacion.RespuestaEvaluacionReconocimientos;
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

@Tag(name = "Internal Recognition")
public interface InternalRecognitionController {

    @Operation(summary = "Evaluar reconocimientos")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "internalApiKey")
    @PostMapping("/evaluar")
    ResponseEntity<RespuestaEvaluacionReconocimientos> evaluate(@Valid @RequestBody SolicitudEvaluacionReconocimientos request);

}
