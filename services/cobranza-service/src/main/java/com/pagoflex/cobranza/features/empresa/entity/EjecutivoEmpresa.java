package com.pagoflex.cobranza.features.empresa.entity;

import jakarta.persistence.*;
import lombok.*;


/// Empresa a la que pertenece cada ejecutivo; se identifica por el correo que viene en el JWT
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ejecutivo_empresa")
public class EjecutivoEmpresa {
    @Id
    @Column(name = "correo", updatable = false, nullable = false, length = 150)
    private String correo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private EmpresaCliente empresa;
}
