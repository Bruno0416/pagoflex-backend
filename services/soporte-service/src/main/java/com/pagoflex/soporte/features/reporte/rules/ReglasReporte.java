package com.pagoflex.soporte.features.reporte.rules;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

/// Reglas de los reportes, sin base de datos para poder probarlas solas
public final class ReglasReporte {
    private static final int DIAS_HABILES_PARA_ATRASO = 2;

    /// un compromiso solo puede tener uno de estos a la vez
    public static final Set<EstadoReporte> ACTIVOS =
            Set.of(EstadoReporte.ABIERTO, EstadoReporte.ASIGNADO, EstadoReporte.EN_ATENCION);

    private ReglasReporte() {
    }

    // días de lunes a viernes después del día del reporte
    public static long diasHabilesEntre(LocalDate desde, LocalDate hasta) {
        long dias = 0;
        for (LocalDate d = desde.plusDays(1); !d.isAfter(hasta); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) dias++;
        }
        return dias;
    }

    // un reporte abierto con más de 2 días hábiles está atrasado RN-23
    public static boolean estaAtrasado(EstadoReporte estado, LocalDate creacion, LocalDate hoy) {
        return estado == EstadoReporte.ABIERTO && diasHabilesEntre(creacion, hoy) > DIAS_HABILES_PARA_ATRASO;
    }

    // último día de creación que ya cuenta como atrasado; sirve para filtrar en la base
    public static LocalDate ultimoDiaAtrasado(LocalDate hoy) {
        LocalDate dia = hoy;
        while (diasHabilesEntre(dia, hoy) <= DIAS_HABILES_PARA_ATRASO) {
            dia = dia.minusDays(1);
        }
        return dia;
    }

    // solo se avanza una etapa: ABIERTO -> ASIGNADO -> EN_ATENCION -> RESUELTO
    public static boolean transicionValida(EstadoReporte desde, EstadoReporte hacia) {
        return desde != null && hacia != null && hacia.ordinal() == desde.ordinal() + 1;
    }

    // REP-0012 -> REP-0013
    public static String siguienteNumero(Collection<String> numeros) {
        int mayor = numeros.stream()
                .map(n -> n.substring("REP-".length()))
                .filter(n -> !n.isEmpty() && n.chars().allMatch(Character::isDigit))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
        return "REP-" + String.format("%04d", mayor + 1);
    }
}
