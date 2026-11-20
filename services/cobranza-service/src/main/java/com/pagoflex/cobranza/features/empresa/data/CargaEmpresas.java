package com.pagoflex.cobranza.features.empresa.data;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.organization.OrganizationType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/// Empresas del anexo 9 y PagoFlex, con su código de registro de demostración.
/// Los códigos solo sirven para el MVP; en un entorno real se cambian y no se publican.
@Component
@Order(1)
@RequiredArgsConstructor
public class CargaEmpresas implements ApplicationRunner {
    private final EmpresaClienteRepository repo;
    private final PasswordEncoder encoder;

    private record Empresa(String codigo, String razonSocial, String nombre, String correo, long recargo,
                           OrganizationType tipo, String codigoRegistro) {
    }

    static final List<Empresa> EMPRESAS = List.of(
            new Empresa("PF-00", "PagoFlex SpA", "PagoFlex", "soporte@pagoflex.ejemplo", 0, OrganizationType.INTERNA, "PF26AG"),
            new Empresa("EC-01", "Inmobiliaria Altos del Maipo SpA", "Altos del Maipo", "atencion@altosmaipo.ejemplo", 8_500, OrganizationType.CLIENTE, "AM26EJ"),
            new Empresa("EC-02", "Corporación Educacional Bosque Nativo", "Colegio Bosque Nativo", "pagos@bosquenativo.ejemplo", 5_000, OrganizationType.CLIENTE, "BN26EJ"),
            new Empresa("EC-03", "Crédito Andino S.A.", "Crédito Andino", "clientes@creditoandino.ejemplo", 12_000, OrganizationType.CLIENTE, "CA26EJ"),
            new Empresa("EC-04", "Seguros Horizonte Sur S.A.", "Horizonte Sur", "contacto@horizontesur.ejemplo", 3_000, OrganizationType.CLIENTE, "HS26EJ"),
            new Empresa("EC-05", "Fundación Manos Abiertas", "Manos Abiertas", "socios@manosabiertas.ejemplo", 0, OrganizationType.CLIENTE, "MA26EJ"),
            new Empresa("EC-06", "Club Deportivo Los Halcones", "Club Los Halcones", "tesoreria@loshalcones.ejemplo", 2_000, OrganizationType.CLIENTE, "LH26EJ"),
            new Empresa("EC-07", "Instituto Técnico Cordillera SpA", "Instituto Cordillera", "finanzas@itcordillera.ejemplo", 6_000, OrganizationType.CLIENTE, "IC26EJ"),
            new Empresa("EC-08", "Arriendos Plaza Oriente Ltda.", "Plaza Oriente", "cobranza@plazaoriente.ejemplo", 7_500, OrganizationType.CLIENTE, "PO26EJ")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (Empresa e : EMPRESAS) {
            // 1. crear las que falten
            EmpresaCliente empresa = repo.findByCodigo(e.codigo()).orElseGet(() -> repo.save(EmpresaCliente.builder()
                    .codigo(e.codigo())
                    .razonSocial(e.razonSocial())
                    .nombreFantasia(e.nombre())
                    .correoAtencion(e.correo())
                    .recargoAtraso(e.recargo())
                    .build()));

            // 2. completar tipo y código en bases creadas antes de esta versión
            if (empresa.getTipo() == null) empresa.setTipo(e.tipo());
            if (empresa.getCodigoRegistroHash() == null) empresa.setCodigoRegistroHash(encoder.encode(e.codigoRegistro()));
        }
    }
}
