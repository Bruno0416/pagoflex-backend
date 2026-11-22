package com.pagoflex.reconocimientos.features.recognition.controller;

import com.pagoflex.reconocimientos.features.recognition.dto.response.UserRecognitionResponse;
import com.pagoflex.reconocimientos.features.recognition.service.RecognitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reconocimientos")
@RequiredArgsConstructor
public class RecognitionControllerImpl implements RecognitionController {
    private final RecognitionService service;

    @Override
    public ResponseEntity<List<UserRecognitionResponse>> mine() {
        return ResponseEntity.ok(service.mine());
    }
}
