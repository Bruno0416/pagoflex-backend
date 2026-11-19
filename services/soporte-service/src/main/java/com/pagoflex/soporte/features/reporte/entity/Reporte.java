package com.pagoflex.soporte.features.reporte.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
                @Index(name = "idx_reporte_usuario", columnList = "correo_usuario")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, columnDefinition = "varchar(20)")
    private EstadoReporte estado;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

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

    /// el usuario se identifica por su correo, que viene en el JWT
    @Column(name = "correo_usuario", nullable = false, length = 150)
    private String correoUsuario;

    @Column(name = "telefono_usuario", length = 20)
    private String telefonoUsuario;

    // ---------- gestión del agente RN-23 ----------
    /// correo del agente responsable desde que lo toma
    @Column(name = "agente", length = 150)
    private String agente;

    @Column(name = "fecha_toma")
    private LocalDateTime fechaToma;

    @Column(name = "respuesta", length = 1000)
    private String respuesta;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
}
