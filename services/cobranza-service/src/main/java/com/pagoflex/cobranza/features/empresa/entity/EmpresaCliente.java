package com.pagoflex.cobranza.features.empresa.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "empresa_cliente")
public class EmpresaCliente {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "codigo", unique = true, nullable = false, length = 10)
    private String codigo;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(name = "nombre_fantasia", nullable = false)
    private String nombreFantasia;

    @Column(name = "correo_atencion", nullable = false)
    private String correoAtencion;

    /// recargo fijo que se suma una sola vez cuando el compromiso vence RN-03
    @Column(name = "recargo_atraso", nullable = false)
    private long recargoAtraso;
}
