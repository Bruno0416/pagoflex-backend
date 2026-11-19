package com.pagoflex.soporte.features.reporte.rules;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;

/// Reglas de los reportes, sin base de datos para poder probarlas solas
public final class ReglasReporte {
    private static final int DIAS_HABILES_PARA_ATRASO = 2;

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
