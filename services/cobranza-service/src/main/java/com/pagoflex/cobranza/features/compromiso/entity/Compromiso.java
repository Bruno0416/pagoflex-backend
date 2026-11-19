package com.pagoflex.cobranza.features.compromiso.entity;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "compromiso",
        indexes = {
                @Index(name = "idx_compromiso_empresa_vencimiento", columnList = "empresa_id, fecha_vencimiento"),
                @Index(name = "idx_compromiso_persona", columnList = "persona_id")
        })
public class Compromiso {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "folio", unique = true, nullable = false, length = 20)
    private String folio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaCliente empresa;

    /// persona de usuarios-service; sin clave foránea entre servicios
    @Column(name = "persona_id", nullable = false)
    private UUID personaId;

    /// rut y nombre al registrar, para listar sin llamar a usuarios
    @Column(name = "persona_rut", nullable = false, length = 12)
    private String personaRut;

    @Column(name = "persona_nombre", nullable = false, length = 170)
    private String personaNombre;

    @Column(name = "concepto", nullable = false)
    private String concepto;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    @Column(name = "total_cuotas")
    private Integer totalCuotas;

    @Column(name = "monto", nullable = false)
    private long monto;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private EstadoCompromiso estado;

    /// recargo ya guardado; mientras está en cero se calcula con el de la empresa
    @Column(name = "recargo_aplicado", nullable = false)
    private long recargoAplicado;

    @Column(name = "fecha_cambio_estado", nullable = false)
    private LocalDateTime fechaCambioEstado;

    /// ejecutivo que lo registró RN-20; nulo en la carga inicial
    @Column(name = "registrado_por_id")
    private UUID registradoPorId;

    @Column(name = "registrado_por_correo", length = 150)
    private String registradoPorCorreo;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @OneToOne(mappedBy = "compromiso", fetch = FetchType.LAZY)
    private Anulacion anulacion;

    @OneToOne(mappedBy = "compromiso", fetch = FetchType.LAZY)
    private Pago pago;

    /// evita que dos pagos o cambios simultáneos pisen el mismo compromiso
    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
