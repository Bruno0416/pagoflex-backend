package com.pagoflex.mensajeria.features.conversacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/// hasta qué mensaje leyó cada participante
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "lectura_conversacion",
        uniqueConstraints = @UniqueConstraint(name = "uk_lectura", columnNames = {"conversacion_id", "usuario_id"}))
public class LecturaConversacion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false, updatable = false)
    private ConversacionTicket conversacion;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "ultima_secuencia_leida", nullable = false)
    private long ultimaSecuenciaLeida;
}
