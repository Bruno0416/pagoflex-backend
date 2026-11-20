package com.pagoflex.cobranza.features.empresa.data;

import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.organization.IdentificadoresEmpresa;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/// Empresas clientes del anexo 9; solo con pagoflex.demo.enabled=true y una sola vez si la tabla está vacía.
/// Su id sale del código para que usuarios-service enlace a sus ejecutivos de ejemplo sin consultar a cobranza
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@Order(1)
@RequiredArgsConstructor
public class CargaEmpresas implements ApplicationRunner {
    private final EmpresaClienteRepository repo;

    @Override
    public void run(ApplicationArguments args) {
        // 1. no repetir la carga si ya hay empresas
        if (repo.count() > 0) return;

        // 2. guardar las empresas con su recargo por atraso
        repo.saveAll(List.of(
                empresa("EC-01", "Inmobiliaria Altos del Maipo SpA", "Altos del Maipo", "atencion@altosmaipo.ejemplo", 8_500),
                empresa("EC-02", "Corporación Educacional Bosque Nativo", "Colegio Bosque Nativo", "pagos@bosquenativo.ejemplo", 5_000),
                empresa("EC-03", "Crédito Andino S.A.", "Crédito Andino", "clientes@creditoandino.ejemplo", 12_000),
                empresa("EC-04", "Seguros Horizonte Sur S.A.", "Horizonte Sur", "contacto@horizontesur.ejemplo", 3_000),
                empresa("EC-05", "Fundación Manos Abiertas", "Manos Abiertas", "socios@manosabiertas.ejemplo", 0),
                empresa("EC-06", "Club Deportivo Los Halcones", "Club Los Halcones", "tesoreria@loshalcones.ejemplo", 2_000),
                empresa("EC-07", "Instituto Técnico Cordillera SpA", "Instituto Cordillera", "finanzas@itcordillera.ejemplo", 6_000),
                empresa("EC-08", "Arriendos Plaza Oriente Ltda.", "Plaza Oriente", "cobranza@plazaoriente.ejemplo", 7_500)
        ));
    }

    // --- helper privado ---
    private EmpresaCliente empresa(String codigo, String razonSocial, String nombre, String correo, long recargo) {
        return EmpresaCliente.builder()
                .id(IdentificadoresEmpresa.deCodigoDemo(codigo))
                .codigo(codigo)
                .razonSocial(razonSocial)
                .nombreFantasia(nombre)
                .correoAtencion(correo)
                .recargoAtraso(recargo)
                .build();
    }
}
