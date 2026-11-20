package com.pagoflex.reconocimientos.features.recognition.service;

import com.pagoflex.reconocimientos.features.recognition.domain.RecognitionCandidate;
import com.pagoflex.reconocimientos.features.recognition.entity.UserRecognition;
import com.pagoflex.reconocimientos.features.recognition.repository.UserRecognitionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class RecognitionGrantService {
    private final UserRecognitionRepository repository;
    private final Clock clock;
    private final TransactionTemplate requiresNew;

    public RecognitionGrantService(UserRecognitionRepository repository, Clock clock,
                                   PlatformTransactionManager manager) {
        this.repository = repository;
        this.clock = clock;
        this.requiresNew = new TransactionTemplate(manager);
        this.requiresNew.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
    }

    // ---------- otorgar desde la evaluacion ----------
    public boolean grantIfAbsent(UUID userId, RecognitionCandidate candidate) {
        // 1. cada otorgamiento va en su transaccion para que un duplicado no afecte a los demas
        try {
            return Boolean.TRUE.equals(requiresNew.execute(status -> insertIfAbsent(userId, candidate)));
        } catch (DataIntegrityViolationException ex) {
            // 2. otro hilo lo otorgo antes: ya existe
            return false;
        }
    }

    // --- helper ---
    private boolean insertIfAbsent(UUID userId, RecognitionCandidate candidate) {
        // 1. omitir si el hecho ya fue reconocido
        if (repository.existsByUserIdAndCodeAndReference(userId, candidate.code(), candidate.reference())) {
            return false;
        }
        // 2. registrar el reconocimiento con la fecha actual
        repository.saveAndFlush(new UserRecognition(userId, candidate.code(), candidate.reference(),
                candidate.reason(), Instant.now(clock)));
        return true;
    }
}
