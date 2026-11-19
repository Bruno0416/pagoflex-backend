package com.pagoflex.reconocimientos.features.recognition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario_reconocimiento",
        uniqueConstraints = @UniqueConstraint(name = "uk_ur_usuario_codigo_ref",
                columnNames = {"id_usuario", "codigo_reconocimiento", "referencia"}),
        indexes = @Index(name = "ix_ur_usuario_fecha", columnList = "id_usuario, fecha_obtencion"))
@Getter
@NoArgsConstructor
public class UserRecognition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_usuario", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "codigo_reconocimiento", length = 40, nullable = false, updatable = false)
    private RecognitionCode code;

    @Column(name = "referencia", length = 60, nullable = false, updatable = false)
    private String reference;

    @Column(name = "motivo", length = 255, nullable = false, updatable = false)
    private String reason;

    @Column(name = "fecha_obtencion", nullable = false, updatable = false)
    private Instant obtainedAt;

    public UserRecognition(UUID userId, RecognitionCode code, String reference, String reason, Instant obtainedAt) {
        this.userId = userId;
        this.code = code;
        this.reference = reference;
        this.reason = reason;
        this.obtainedAt = obtainedAt;
    }
}
