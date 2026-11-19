package com.pagoflex.soporte.features.reporte.data;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/// Reportes del anexo 9, los mismos que trae la app; los cobros coinciden con la carga demo de cobranza.
/// Solo se cargan con pagoflex.demo.enabled=true y si no hay reportes
@Component
@ConditionalOnProperty(name = "pagoflex.demo.enabled", havingValue = "true")
@RequiredArgsConstructor
public class CargaDemo implements ApplicationRunner {
    private final ReporteRepository repository;

    @Override
    public void run(ApplicationArguments args) {
        // 1. no repetir la carga
        if (repository.count() > 0) return;

        // 2. uno resuelto, uno en gestión y uno abierto
        Reporte rep9 = base("REP-0009", "CP-2026-0111", MotivoReporte.MONTO_INCORRECTO,
                "Repacté el crédito y me siguen cobrando la cuota antigua.", "2026-09-18", EstadoReporte.RESUELTO,
                "Crédito Andino", "Crédito de consumo", 19, 36, 54_200, "2026-09-20", "ANULADO",
                "Héctor Manuel Pizarro Campos", "hpizarro@correo.ejemplo", "+56 9 0000 0106");
        rep9.setAgente("alagos@pagoflex.ejemplo");
        rep9.setFechaToma(LocalDate.of(2026, 9, 18).atTime(12, 0));
        rep9.setRespuesta("La empresa anuló la cuota por repactación (22-09-2026).");
        rep9.setFechaResolucion(LocalDate.of(2026, 9, 22).atStartOfDay());

        Reporte rep11 = base("REP-0011", "CP-2026-0109", MotivoReporte.YA_PAGUE,
                "Pagué por transferencia directa al instituto el 03-10.", "2026-10-03", EstadoReporte.EN_GESTION,
                "Instituto Cordillera", "Arancel octubre", 8, 10, 148_000, "2026-10-05", "EN_REVISION",
                "Matías Ignacio Soto Vergara", "msoto@correo.ejemplo", "+56 9 0000 0104");
        rep11.setAgente("dortiz@pagoflex.ejemplo");
        rep11.setFechaToma(LocalDate.of(2026, 10, 3).atTime(15, 0));

        Reporte rep12 = base("REP-0012", "CP-2026-0106", MotivoReporte.MONTO_INCORRECTO,
                "No sé por qué me cobran $318.500.", "2026-10-05", EstadoReporte.ABIERTO,
                "Altos del Maipo", "Cuota pie departamento", 14, 36, 318_500, "2026-09-30", "EN_REVISION",
                "Luis Alberto Muñoz Tapia", "lmunoz@correo.ejemplo", "+56 9 0000 0102");

        repository.saveAll(List.of(rep9, rep11, rep12));
    }

    // --- helper privado ---
    private Reporte base(String numero, String folio, MotivoReporte motivo, String comentario, String fecha,
                         EstadoReporte estado, String empresa, String concepto, Integer cuota, Integer totalCuotas,
                         long total, String vence, String estadoCompromiso,
                         String nombre, String correo, String telefono) {
        return Reporte.builder()
                .numero(numero)
                .folio(folio)
                .motivo(motivo)
                .comentario(comentario)
                .estado(estado)
                .fechaCreacion(LocalDate.parse(fecha).atStartOfDay())
                .empresa(empresa)
                .concepto(concepto)
                .numeroCuota(cuota)
                .totalCuotas(totalCuotas)
                .total(total)
                .fechaVencimiento(LocalDate.parse(vence))
                .estadoCompromiso(estadoCompromiso)
                .nombreUsuario(nombre)
                .correoUsuario(correo)
                .telefonoUsuario(telefono)
                .build();
    }
}
