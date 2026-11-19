package com.pagoflex.cobranza.features.empresa.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/// Empresa de cada ejecutivo, por el id de su cuenta (sub del JWT)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ejecutivo_empresa")
public class EjecutivoEmpresa {
    @Id
    @Column(name = "usuario_id", updatable = false, nullable = false)
    private UUID usuarioId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaCliente empresa;
}
