package com.pagoflex.cobranza.features.compromiso.rules;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/// Reglas de negocio de los compromisos, sin base de datos para poder probarlas solas
public final class ReglasCompromiso {
    private static final Set<EstadoCompromiso> ANULABLES =
            EnumSet.of(EstadoCompromiso.PENDIENTE, EstadoCompromiso.VENCIDO, EstadoCompromiso.EN_REVISION);

    private ReglasCompromiso() {
    }

    // vence a las 23:59 del día; al día siguiente pasa a vencido RN-02
    public static EstadoCompromiso estadoSegunFecha(LocalDate vencimiento, LocalDate hoy) {
        return hoy.isAfter(vencimiento) ? EstadoCompromiso.VENCIDO : EstadoCompromiso.PENDIENTE;
    }

    // un pendiente guardado que ya venció se muestra como vencido
    public static EstadoCompromiso estadoEfectivo(EstadoCompromiso guardado, LocalDate vencimiento, LocalDate hoy) {
        return guardado == EstadoCompromiso.PENDIENTE ? estadoSegunFecha(vencimiento, hoy) : guardado;
    }

    // el recargo fijo de la empresa se suma una sola vez al vencer RN-03
    public static long recargoEfectivo(EstadoCompromiso estado, long recargoAplicado, long recargoEmpresa) {
        if (recargoAplicado > 0) return recargoAplicado;
        return estado == EstadoCompromiso.VENCIDO ? recargoEmpresa : 0;
    }

    public static long diasDeAtraso(LocalDate vencimiento, LocalDate hoy) {
        return Math.max(0, ChronoUnit.DAYS.between(vencimiento, hoy));
    }

    // solo se anulan pendientes, vencidos o en revisión; un pagado nunca RN-21
    public static boolean puedeAnularse(EstadoCompromiso estado) {
        return ANULABLES.contains(estado);
    }

    // CP-2026-0112 -> CP-2026-0113
    public static String siguienteFolio(Collection<String> folios, int anio) {
        String prefijo = "CP-" + anio + "-";
        int mayor = folios.stream()
                .filter(f -> f.startsWith(prefijo))
                .map(f -> f.substring(prefijo.length()))
                .filter(n -> n.chars().allMatch(Character::isDigit) && !n.isEmpty())
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return prefijo + String.format("%04d", mayor + 1);
    }

    // tasa = pagados / (compromisos del mes - anulados) * 100, con un decimal RN-24
    public static double tasaCobranza(int pagados, int total, int anulados) {
        int base = total - anulados;
        if (base <= 0) return 0.0;
        return Math.round(pagados * 1000.0 / base) / 10.0;
    }
}
