package com.pagoflex.reconocimientos.features.recognition.repository;

import com.pagoflex.reconocimientos.features.recognition.entity.RecognitionCode;
import com.pagoflex.reconocimientos.features.recognition.entity.UserRecognition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserRecognitionRepository extends JpaRepository<UserRecognition, Long> {
    boolean existsByUserIdAndCodeAndReference(UUID userId, RecognitionCode code, String reference);

    List<UserRecognition> findByUserIdOrderByObtainedAtDescIdDesc(UUID userId);
}
