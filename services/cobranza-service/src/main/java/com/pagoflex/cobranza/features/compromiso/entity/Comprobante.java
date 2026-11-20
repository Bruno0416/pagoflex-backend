package com.pagoflex.cobranza.features.compromiso.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/// copia fija de los datos al momento del pago; nunca se modifica RN-06
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@Table(name = "comprobante")
public class Comprobante {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero", unique = true, nullable = false, length = 20)
    private String numero;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pago_id", unique = true, nullable = false)
    private Pago pago;

    @Column(name = "folio", nullable = false, length = 20)
    private String folio;

    @Column(name = "empresa", nullable = false)
    private String empresa;

    @Column(name = "concepto", nullable = false)
    private String concepto;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    @Column(name = "total_cuotas")
    private Integer totalCuotas;

    @Column(name = "persona_nombre", nullable = false, length = 170)
    private String personaNombre;

    @Column(name = "persona_rut", nullable = false, length = 12)
    private String personaRut;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "monto", nullable = false)
    private long monto;

    @Column(name = "recargo", nullable = false)
    private long recargo;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "canal", nullable = false, length = 30)
    private String canal;

    @Column(name = "medio", nullable = false, length = 20)
    private String medio;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;
}
