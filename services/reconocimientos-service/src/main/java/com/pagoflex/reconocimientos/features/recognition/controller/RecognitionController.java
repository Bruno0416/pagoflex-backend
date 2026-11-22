package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.reconocimientos.features.recognition.dto.response.UserRecognitionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Recognition")
public interface RecognitionController {

    @Operation(summary = "Listar mis reconocimientos")
    @ApiResponse(responseCode = "200", description = "Operación exitosa")
    @ApiResponse(responseCode = "400", description = "Solicitud inválida")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/me")
    ResponseEntity<List<UserRecognitionResponse>> mine();

}
