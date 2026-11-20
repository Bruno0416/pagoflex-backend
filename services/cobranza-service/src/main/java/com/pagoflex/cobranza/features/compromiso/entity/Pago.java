package com.pagoflex.cobranza.features.compromiso.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/// pago simulado: uno solo por compromiso y total RN-05
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "pago",
        uniqueConstraints = @UniqueConstraint(name = "uk_pago_idempotencia", columnNames = {"usuario_id", "idempotency_key"}))
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "compromiso_id", unique = true, nullable = false, updatable = false)
    private Compromiso compromiso;

    /// cuenta que pagó; nulo en pagos de la carga inicial o de otros canales
    @Column(name = "usuario_id", updatable = false)
    private UUID usuarioId;

    @Column(name = "idempotency_key", length = 64, updatable = false)
    private String idempotencyKey;

    @Column(name = "fecha_hora", nullable = false, updatable = false)
    private LocalDateTime fechaHora;

    @Column(name = "monto_pagado", nullable = false, updatable = false)
    private long montoPagado;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, updatable = false, columnDefinition = "varchar(30)")
    private CanalPago canal;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, updatable = false, columnDefinition = "varchar(20)")
    private MedioPago medio;

    @OneToOne(mappedBy = "pago", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Comprobante comprobante;
}
