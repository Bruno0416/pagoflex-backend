package com.pagoflex.cobranza.features.compromiso.rules;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso.*;
import static org.assertj.core.api.Assertions.assertThat;

class ReglasCompromisoTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 6);

    @Test
    void venceAlDiaSiguienteYElRecargoSeSumaUnaVez() {
        assertThat(ReglasCompromiso.estadoEfectivo(PENDIENTE, HOY, HOY)).isEqualTo(PENDIENTE);
        assertThat(ReglasCompromiso.estadoEfectivo(PENDIENTE, HOY.minusDays(1), HOY)).isEqualTo(VENCIDO);
        assertThat(ReglasCompromiso.estadoEfectivo(PAGADO, HOY.minusDays(9), HOY)).isEqualTo(PAGADO);
        assertThat(ReglasCompromiso.recargoEfectivo(VENCIDO, 0, 8_500)).isEqualTo(8_500);
        assertThat(ReglasCompromiso.recargoEfectivo(VENCIDO, 2_000, 8_500)).isEqualTo(2_000);
        assertThat(ReglasCompromiso.recargoEfectivo(PENDIENTE, 0, 8_500)).isZero();
    }

    @Test
    void soloSePaganPendientesOVencidosYNuncaSeAnulaUnPagado() {
        assertThat(ReglasCompromiso.puedePagarse(PENDIENTE)).isTrue();
        assertThat(ReglasCompromiso.puedePagarse(VENCIDO)).isTrue();
        assertThat(ReglasCompromiso.puedePagarse(EN_REVISION)).isFalse();
        assertThat(ReglasCompromiso.puedePagarse(ANULADO)).isFalse();
        assertThat(ReglasCompromiso.puedeAnularse(PAGADO)).isFalse();
        assertThat(ReglasCompromiso.puedeAnularse(EN_REVISION)).isTrue();
    }

    @Test
    void primeroSePagaLaCuotaAnteriorDelPlan() {
        var c1 = cuota("CP-1", 1, HOY.plusDays(5), PENDIENTE);
        var c2 = cuota("CP-2", 2, HOY.plusDays(35), PENDIENTE);
        var otraEmpresaPagada = cuota("CP-0", 0, HOY.minusDays(30), PAGADO);

        assertThat(ReglasCompromiso.folioAnteriorPorPagar(c2, List.of(c1, c2, otraEmpresaPagada))).contains("CP-1");
        assertThat(ReglasCompromiso.folioAnteriorPorPagar(c1, List.of(c1, c2))).isEmpty();
        // pagada la primera, la segunda queda libre
        var c1Pagada = cuota("CP-1", 1, HOY.plusDays(5), PAGADO);
        assertThat(ReglasCompromiso.folioAnteriorPorPagar(c2, List.of(c1Pagada, c2))).isEmpty();
        // una cuota en revisión no bloquea a la siguiente
        var c1Revision = cuota("CP-1", 1, HOY.plusDays(5), EN_REVISION);
        assertThat(ReglasCompromiso.folioAnteriorPorPagar(c2, List.of(c1Revision, c2))).isEmpty();
    }

    @Test
    void foliosYComprobantesCorrelativos() {
        assertThat(ReglasCompromiso.siguienteFolio(List.of("CP-2026-0112", "CP-2025-0900"), 2026)).isEqualTo("CP-2026-0113");
        assertThat(ReglasCompromiso.siguienteFolio(List.of(), 2026)).isEqualTo("CP-2026-0001");
        assertThat(ReglasCompromiso.siguienteComprobante(List.of("CPR-000451"))).isEqualTo("CPR-000452");
    }

    @Test
    void laCotizacionCambiaSiCambiaElTotalOElDia() {
        String hoy = ReglasCompromiso.cotizacion("CP-1", 100_000, HOY);
        assertThat(ReglasCompromiso.cotizacion("CP-1", 100_000, HOY)).isEqualTo(hoy);
        assertThat(ReglasCompromiso.cotizacion("CP-1", 108_500, HOY)).isNotEqualTo(hoy);
        assertThat(ReglasCompromiso.cotizacion("CP-1", 100_000, HOY.plusDays(1))).isNotEqualTo(hoy);
    }

    @Test
    void tasaDeCobranzaSinAnulados() {
        assertThat(ReglasCompromiso.tasaCobranza(104, 120, 2)).isEqualTo(88.1);
        assertThat(ReglasCompromiso.tasaCobranza(0, 2, 2)).isZero();
    }

    @Test
    void cumplimientoConMinimoDeTresYCategoria() {
        var aTiempo1 = pagada("A", HOY.minusDays(40), HOY.minusDays(41));
        var aTiempo2 = pagada("B", HOY.minusDays(20), HOY.minusDays(20));
        var tarde = pagada("C", HOY.minusDays(10), HOY.minusDays(5));
        var anulada = new ReglasCompromiso.Cuota("D", "x", null, null, HOY.minusDays(3), ANULADO, null);

        var dos = ReglasCompromiso.cumplimiento(List.of(aTiempo1, aTiempo2, anulada), HOY);
        assertThat(dos.elegibles()).isEqualTo(2);
        assertThat(dos.porcentaje()).isNull();

        var tres = ReglasCompromiso.cumplimiento(List.of(aTiempo1, aTiempo2, tarde, anulada), HOY);
        assertThat(tres.elegibles()).isEqualTo(3);
        assertThat(tres.porcentaje()).isEqualTo(66.7);
        assertThat(tres.categoria()).isEqualTo("EN_CAMINO");
    }

    // --- helpers ---
    private static ReglasCompromiso.Cuota cuota(String folio, int numero, LocalDate vence, EstadoCompromiso estado) {
        return new ReglasCompromiso.Cuota(folio, "Cuota pie", numero, 36, vence, estado, null);
    }

    private static ReglasCompromiso.Cuota pagada(String folio, LocalDate vence, LocalDate pago) {
        return new ReglasCompromiso.Cuota(folio, "x", null, null, vence, PAGADO, pago);
    }
}
