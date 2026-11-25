package com.pagoflex.soporte.features.reporte.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "reporte",
        indexes = {
                @Index(name = "idx_reporte_folio_estado", columnList = "folio, estado"),
                @Index(name = "idx_reporte_usuario", columnList = "usuario_id"),
                @Index(name = "idx_reporte_empresa", columnList = "empresa_id"),
                @Index(name = "idx_reporte_agente", columnList = "agente_id")
        })
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "numero", unique = true, nullable = false, length = 20)
    private String numero;

    /// folio del compromiso en cobranza-service
    @Column(name = "folio", nullable = false, length = 20)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo", nullable = false, columnDefinition = "varchar(20)")
    private MotivoReporte motivo;

    @Column(name = "comentario", length = 500)
    private String comentario;

    /// nombre del archivo de la foto del comprobante de pago, si la usuaria la subió
    @Column(name = "comprobante", length = 60)
    private String comprobante;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private EstadoReporte estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    // ---------- dueños por uuid: usuario de usuarios-service y empresa de cobranza ----------
    /// Users.id del JWT de quien reportó
    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    // ---------- copia del cobro al reportar, para la bandeja del agente ----------
    @Column(name = "empresa", nullable = false)
    private String empresa;

    @Column(name = "concepto", nullable = false)
    private String concepto;

    @Column(name = "numero_cuota")
    private Integer numeroCuota;

    @Column(name = "total_cuotas")
    private Integer totalCuotas;

    /// estado del cobro en cobranza la última vez que soporte lo tocó
    @Column(name = "estado_compromiso", length = 20)
    private String estadoCompromiso;

    @Column(name = "total", nullable = false)
    private long total;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @Column(name = "nombre_usuario", nullable = false)
    private String nombreUsuario;

    @Column(name = "usuario_correo", length = 150)
    private String usuarioCorreo;

    @Column(name = "telefono_usuario", length = 20)
    private String telefonoUsuario;

    // ---------- gestión del agente ----------
    @Column(name = "agente_id")
    private UUID agenteId;

    @Column(name = "agente_correo", length = 150)
    private String agenteCorreo;

    /// cuando el agente lo tomó (ASIGNADO)
    @Column(name = "fecha_toma")
    private LocalDateTime fechaToma;

    @Column(name = "fecha_atencion")
    private LocalDateTime fechaAtencion;

    @Column(name = "respuesta", length = 1000)
    private String respuesta;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Builder.Default
    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<HistorialEstadoReporte> historial = new ArrayList<>();

    // ---------- cambios de estado ----------
    public void registrarCambio(EstadoReporte nuevo, UUID actorId, String actorCorreo, LocalDateTime fecha) {
        // 1. guardar el paso en el historial
        historial.add(HistorialEstadoReporte.builder()
                .reporte(this)
                .estadoAnterior(estado)
                .estadoNuevo(nuevo)
                .actorId(actorId)
                .actorCorreo(actorCorreo)
                .fecha(fecha)
                .build());
        // 2. dejar el estado nuevo
        this.estado = nuevo;
    }
}
