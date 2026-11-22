package com.pagoflex.cobranza.features.compromiso.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/// quién anuló, por qué y cuándo RN-21
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "anulacion")
public class Anulacion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compromiso_id", unique = true, nullable = false)
    private Compromiso compromiso;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, columnDefinition = "varchar(20)")
    private MotivoAnulacion motivo;

    @Column(name = "comentario", length = 500)
    private String comentario;

    @Column(name = "ejecutivo_id")
    private UUID ejecutivoId;

    @Column(name = "ejecutivo_correo", nullable = false, length = 150)
    private String ejecutivoCorreo;

    @Column(name = "fecha", nullable = false)
    private LocalDateTime fecha;
}
