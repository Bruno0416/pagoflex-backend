package com.pagoflex.cobranza.features.empresa.entity;

import com.pagoflex.contracts.organization.OrganizationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    /// INTERNA es PagoFlex (registra agentes); CLIENTE registra ejecutivos
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", columnDefinition = "varchar(10)")
    private OrganizationType tipo;

    /// solo el hash del código de registro; el código nunca se guarda en claro
    @Column(name = "codigo_registro_hash", length = 100)
    private String codigoRegistroHash;
}
