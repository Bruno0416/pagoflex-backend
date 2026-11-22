package com.pagoflex.mensajeria.features.conversacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/// Conversación interna entre el agente y el ejecutivo sobre un reporte
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "conversacion_ticket")
public class ConversacionTicket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "reporte_numero", unique = true, nullable = false, length = 20)
    private String reporteNumero;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    /// cuando el reporte se resuelve la conversación queda solo de lectura
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /// última secuencia usada; ordena los mensajes sin depender del reloj del celular
    @Column(name = "ultima_secuencia", nullable = false)
    private long ultimaSecuencia;
}
