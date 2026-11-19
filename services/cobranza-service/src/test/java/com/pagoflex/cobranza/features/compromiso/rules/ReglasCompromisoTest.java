package com.pagoflex.cobranza.features.compromiso.rules;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasCompromisoTest {
    private final LocalDate hoy = LocalDate.of(2026, 10, 6);

    // ---------- estado y recargo ----------
    @Test
    void dueDayIsPendingAndNextDayIsOverdue() {
        LocalDate vence = LocalDate.of(2026, 10, 5);
        assertThat(ReglasCompromiso.estadoSegunFecha(vence, vence)).isEqualTo(EstadoCompromiso.PENDIENTE);
        assertThat(ReglasCompromiso.estadoSegunFecha(vence, vence.plusDays(1))).isEqualTo(EstadoCompromiso.VENCIDO);
    }

    @Test
    void storedPendingPastDueIsShownAsOverdueWithSurcharge() {
        EstadoCompromiso estado = ReglasCompromiso.estadoEfectivo(EstadoCompromiso.PENDIENTE, LocalDate.of(2026, 9, 30), hoy);

        assertThat(estado).isEqualTo(EstadoCompromiso.VENCIDO);
        assertThat(ReglasCompromiso.recargoEfectivo(estado, 0, 8_500)).isEqualTo(8_500);
        assertThat(ReglasCompromiso.estadoEfectivo(EstadoCompromiso.PAGADO, LocalDate.of(2026, 9, 30), hoy))
                .isEqualTo(EstadoCompromiso.PAGADO);
    }

    @Test
    void savedSurchargeIsNotAppliedTwice() {
        assertThat(ReglasCompromiso.recargoEfectivo(EstadoCompromiso.VENCIDO, 8_500, 8_500)).isEqualTo(8_500);
        assertThat(ReglasCompromiso.recargoEfectivo(EstadoCompromiso.PENDIENTE, 0, 8_500)).isZero();
    }

    @Test
    void delayDaysNeverNegative() {
        assertThat(ReglasCompromiso.diasDeAtraso(LocalDate.of(2026, 9, 30), hoy)).isEqualTo(6);
        assertThat(ReglasCompromiso.diasDeAtraso(LocalDate.of(2026, 10, 30), hoy)).isZero();
    }

    // ---------- anulación ----------
    @Test
    void paidOrCancelledCannotBeCancelled() {
        assertThat(ReglasCompromiso.puedeAnularse(EstadoCompromiso.PENDIENTE)).isTrue();
        assertThat(ReglasCompromiso.puedeAnularse(EstadoCompromiso.VENCIDO)).isTrue();
        assertThat(ReglasCompromiso.puedeAnularse(EstadoCompromiso.EN_REVISION)).isTrue();
        assertThat(ReglasCompromiso.puedeAnularse(EstadoCompromiso.PAGADO)).isFalse();
        assertThat(ReglasCompromiso.puedeAnularse(EstadoCompromiso.ANULADO)).isFalse();
    }

    // ---------- folio y tasa ----------
    @Test
    void nextFolioContinuesTheYearSequence() {
        assertThat(ReglasCompromiso.siguienteFolio(List.of("CP-2026-0111", "CP-2026-0112"), 2026)).isEqualTo("CP-2026-0113");
        assertThat(ReglasCompromiso.siguienteFolio(List.of("CP-2025-0900"), 2026)).isEqualTo("CP-2026-0001");
    }

    @Test
    void collectionRateExcludesCancelled() {
        // ejemplo del anexo: 120 compromisos, 2 anulados y 104 pagados
        assertThat(ReglasCompromiso.tasaCobranza(104, 120, 2)).isEqualTo(88.1);
        assertThat(ReglasCompromiso.tasaCobranza(0, 0, 0)).isZero();
    }
}
