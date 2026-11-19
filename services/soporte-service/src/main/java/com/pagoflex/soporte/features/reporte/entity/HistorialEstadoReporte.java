package com.pagoflex.soporte.features.reporte.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/// cada cambio de estado del reporte con quién lo hizo y cuándo
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "historial_estado_reporte",
        indexes = @Index(name = "idx_historial_reporte", columnList = "reporte_id"))
public class HistorialEstadoReporte {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false)
    private Reporte reporte;

    /// nulo cuando el usuario crea el reporte
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", columnDefinition = "varchar(20)")
    private EstadoReporte estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, columnDefinition = "varchar(20)")
    private EstadoReporte estadoNuevo;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "actor_correo", length = 150)
    private String actorCorreo;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;
}
