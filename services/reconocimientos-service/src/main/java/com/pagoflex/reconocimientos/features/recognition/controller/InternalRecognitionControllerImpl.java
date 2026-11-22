package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.contracts.vinculacion.SolicitudEvaluacionReconocimientos;
import com.pagoflex.contracts.vinculacion.RespuestaEvaluacionReconocimientos;
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
public class InternalRecognitionControllerImpl implements InternalRecognitionController {
    private final RecognitionService service;

    @Override
    public ResponseEntity<RespuestaEvaluacionReconocimientos> evaluate(
            @Valid @RequestBody SolicitudEvaluacionReconocimientos request
    ) {
        return ResponseEntity.ok(service.evaluate(request));
    }
}
