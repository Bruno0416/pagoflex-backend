package com.pagoflex.mensajeria.features.conversacion.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mensaje",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_mensaje_secuencia", columnNames = {"conversacion_id", "secuencia"}),
                @UniqueConstraint(name = "uk_mensaje_cliente", columnNames = {"conversacion_id", "autor_id", "client_message_id"})
        })
public class Mensaje {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversacion_id", nullable = false, updatable = false)
    private ConversacionTicket conversacion;

    @Column(name = "autor_id", nullable = false, updatable = false)
    private UUID autorId;

    @Column(name = "autor_correo", nullable = false, updatable = false, length = 150)
    private String autorCorreo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, updatable = false, columnDefinition = "varchar(20)")
    private RolParticipante rol;

    @Column(name = "texto", nullable = false, updatable = false, length = 1000)
    private String texto;

    /// fecha del servidor, nunca la del celular
    @Column(name = "enviado_en", nullable = false, updatable = false)
    private LocalDateTime enviadoEn;

    /// id que genera la app para no duplicar un mensaje al reintentar
    @Column(name = "client_message_id", nullable = false, updatable = false, length = 64)
    private String clientMessageId;

    @Column(name = "secuencia", nullable = false, updatable = false)
    private long secuencia;
}
