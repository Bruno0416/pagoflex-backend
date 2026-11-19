package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsRequest;
import com.pagoflex.contracts.vinculacion.EvaluateRecognitionsResponse;
import com.pagoflex.reconocimientos.features.recognition.service.RecognitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/v1/internal/reconocimientos")
@RequiredArgsConstructor
public class InternalRecognitionController {
    private final RecognitionService service;

    @PostMapping("/evaluar")
    public ResponseEntity<EvaluateRecognitionsResponse> evaluate(
            @Valid @RequestBody EvaluateRecognitionsRequest request
    ) {
        return ResponseEntity.ok(service.evaluate(request));
    }
}
