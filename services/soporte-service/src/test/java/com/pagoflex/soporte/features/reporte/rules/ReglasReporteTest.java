package com.pagoflex.soporte.features.reporte.rules;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasReporteTest {
    // 06-10-2026 es martes
    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    @Test
    void businessDaysSkipTheWeekend() {
        // viernes 02 -> martes 06: lunes y martes
        assertThat(ReglasReporte.diasHabilesEntre(LocalDate.of(2026, 10, 2), HOY)).isEqualTo(2);
        // jueves 01 -> martes 06: viernes, lunes y martes
        assertThat(ReglasReporte.diasHabilesEntre(LocalDate.of(2026, 10, 1), HOY)).isEqualTo(3);
        assertThat(ReglasReporte.diasHabilesEntre(HOY, HOY)).isZero();
    }

    @Test
    void onlyOpenReportsOverTwoBusinessDaysAreLate() {
        LocalDate jueves = LocalDate.of(2026, 10, 1);
        LocalDate viernes = LocalDate.of(2026, 10, 2);

        assertThat(ReglasReporte.estaAtrasado(EstadoReporte.ABIERTO, jueves, HOY)).isTrue();
        assertThat(ReglasReporte.estaAtrasado(EstadoReporte.ABIERTO, viernes, HOY)).isFalse();
        // si ya lo tomó un agente deja de contar como atrasado
        assertThat(ReglasReporte.estaAtrasado(EstadoReporte.ASIGNADO, jueves, HOY)).isFalse();
        assertThat(ReglasReporte.estaAtrasado(EstadoReporte.RESUELTO, jueves, HOY)).isFalse();
        // el límite para la base coincide con la regla
        assertThat(ReglasReporte.ultimoDiaAtrasado(HOY)).isEqualTo(jueves);
    }

    @Test
    void statesOnlyMoveOneStepForward() {
        assertThat(ReglasReporte.transicionValida(EstadoReporte.ABIERTO, EstadoReporte.ASIGNADO)).isTrue();
        assertThat(ReglasReporte.transicionValida(EstadoReporte.ASIGNADO, EstadoReporte.EN_ATENCION)).isTrue();
        assertThat(ReglasReporte.transicionValida(EstadoReporte.EN_ATENCION, EstadoReporte.RESUELTO)).isTrue();
        // sin saltos ni retrocesos
        assertThat(ReglasReporte.transicionValida(EstadoReporte.ABIERTO, EstadoReporte.RESUELTO)).isFalse();
        assertThat(ReglasReporte.transicionValida(EstadoReporte.ASIGNADO, EstadoReporte.RESUELTO)).isFalse();
        assertThat(ReglasReporte.transicionValida(EstadoReporte.EN_ATENCION, EstadoReporte.ASIGNADO)).isFalse();
        assertThat(ReglasReporte.transicionValida(EstadoReporte.RESUELTO, EstadoReporte.ABIERTO)).isFalse();
    }

    @Test
    void nextNumberFollowsTheHighest() {
        assertThat(ReglasReporte.siguienteNumero(List.of())).isEqualTo("REP-0001");
        assertThat(ReglasReporte.siguienteNumero(List.of("REP-0002", "REP-0012", "REP-0009"))).isEqualTo("REP-0013");
    }
}
