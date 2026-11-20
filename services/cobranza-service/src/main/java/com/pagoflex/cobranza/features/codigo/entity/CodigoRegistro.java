package com.pagoflex.cobranza.features.codigo.entity;

import com.pagoflex.contracts.organization.AuthorizedRole;
import com.pagoflex.contracts.organization.OrganizationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/// Código privado de registro de agentes y ejecutivos. Nunca guarda el código: solo su prefijo público
/// (para buscarlo) y el hash BCrypt del código completo. No es el identificador público EmpresaCliente.codigo
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "codigo_registro")
public class CodigoRegistro {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "prefijo", unique = true, nullable = false, updatable = false, length = 6)
    private String prefijo;

    @Column(name = "hash", nullable = false, updatable = false, length = 100)
    private String hash;

    /// empresa cliente o, para las internas, la organización de PagoFlex
    @Column(name = "empresa_id", nullable = false, updatable = false)
    private UUID empresaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, updatable = false, columnDefinition = "varchar(10)")
    private OrganizationType tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, updatable = false, columnDefinition = "varchar(10)")
    private AuthorizedRole rol;

    @Column(name = "activo", nullable = false)
    private boolean activo;

    /// nulo si no vence
    @Column(name = "expira_en")
    private Instant expiraEn;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;
}
