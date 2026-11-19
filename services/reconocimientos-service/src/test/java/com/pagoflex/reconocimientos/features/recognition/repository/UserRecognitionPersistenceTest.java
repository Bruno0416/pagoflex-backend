package com.pagoflex.reconocimientos.features.recognition.repository;

import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;
import com.pagoflex.reconocimientos.features.recognition.entity.UserRecognition;
import com.pagoflex.reconocimientos.features.recognition.domain.RecognitionCandidate;
import com.pagoflex.reconocimientos.features.recognition.service.RecognitionGrantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class UserRecognitionPersistenceTest {
    @Autowired
    UserRecognitionRepository repository;

    @Autowired
    RecognitionGrantService grants;

    // ---------- unicidad ----------
    @Test
    void duplicateGrantViolatesUniqueKey() {
        UUID user = UUID.randomUUID();
        repository.saveAndFlush(row(user, RecognitionCode.PRIMER_PAGO_A_TIEMPO, "-"));
        assertThatThrownBy(() -> repository.saveAndFlush(row(user, RecognitionCode.PRIMER_PAGO_A_TIEMPO, "-")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void grantIfAbsentIsIdempotent() {
        UUID user = UUID.randomUUID();
        var candidate = new RecognitionCandidate(RecognitionCode.TRES_MESES_AL_DIA, "-", "motivo");
        assertThat(grants.grantIfAbsent(user, candidate)).isTrue();
        assertThat(grants.grantIfAbsent(user, candidate)).isFalse();
        assertThat(repository.findByUserIdOrderByObtainedAtDescIdDesc(user)).hasSize(1);
    }

    @Test
    void concurrentGrantCreatesSingleRow() throws Exception {
        // 1. varios hilos otorgan el mismo reconocimiento a la vez
        UUID user = UUID.randomUUID();
        var candidate = new RecognitionCandidate(RecognitionCode.PRIMER_PAGO_A_TIEMPO, "-", "motivo");
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return grants.grantIfAbsent(user, candidate);
            }));
        }
        start.countDown();
        // 2. exactamente un hilo lo crea y el resto lo reconoce como existente
        long created = 0;
        for (Future<Boolean> result : results) if (result.get()) created++;
        pool.shutdown();
        assertThat(created).isEqualTo(1);
        assertThat(repository.findByUserIdOrderByObtainedAtDescIdDesc(user)).hasSize(1);
    }

    // --- helper ---
    private static UserRecognition row(UUID user, RecognitionCode code, String reference) {
        return new UserRecognition(user, code, reference, "motivo", Instant.now());
    }
}
