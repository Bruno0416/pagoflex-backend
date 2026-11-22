package com.pagoflex.cobranza.features.compromiso.rules;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/// Reglas de negocio de los compromisos, sin base de datos para poder probarlas solas
public final class ReglasCompromiso {
    private static final Set<EstadoCompromiso> ANULABLES =
            EnumSet.of(EstadoCompromiso.PENDIENTE, EstadoCompromiso.VENCIDO, EstadoCompromiso.EN_REVISION);
    private static final Set<EstadoCompromiso> PAGABLES =
            EnumSet.of(EstadoCompromiso.PENDIENTE, EstadoCompromiso.VENCIDO);

    /// lo mínimo de un compromiso para decidir orden y cumplimiento
    public record Cuota(String folio, String concepto, Integer numeroCuota, Integer totalCuotas,
                        LocalDate vencimiento, EstadoCompromiso estado, LocalDate fechaPago) {
    }

    public record Cumplimiento(int elegibles, int pagadosATiempo, Double porcentaje, String categoria) {
    }

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

    // en revisión, anulado o pagado no se paga RN-08/09
    public static boolean puedePagarse(EstadoCompromiso estado) {
        return PAGABLES.contains(estado);
    }

    // primero se paga lo que vence antes y, en un plan, la cuota anterior RN-04
    public static Optional<String> folioAnteriorPorPagar(Cuota objetivo, List<Cuota> deLaEmpresa) {
        return deLaEmpresa.stream()
                .filter(c -> !c.folio().equals(objetivo.folio()))
                .filter(c -> PAGABLES.contains(c.estado()))
                .filter(c -> c.vencimiento().isBefore(objetivo.vencimiento()) || cuotaAnteriorDelPlan(c, objetivo))
                .min((a, b) -> a.vencimiento().compareTo(b.vencimiento()))
                .map(Cuota::folio);
    }

    // CP-2026-0112 -> CP-2026-0113
    public static String siguienteFolio(Collection<String> folios, int anio) {
        String prefijo = "CP-" + anio + "-";
        return prefijo + String.format("%04d", mayorCorrelativo(folios, prefijo) + 1);
    }

    // CPR-000451 -> CPR-000452
    public static String siguienteComprobante(Collection<String> numeros) {
        return String.format("CPR-%06d", mayorCorrelativo(numeros, "CPR-") + 1);
    }

    // la cotización cambia si cambia el total o el día; así se detecta un monto viejo
    public static String cotizacion(String folio, long total, LocalDate dia) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest((folio + "|" + total + "|" + dia).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    // tasa = pagados / (compromisos del mes - anulados) * 100, con un decimal RN-24
    public static double tasaCobranza(int pagados, int total, int anulados) {
        int base = total - anulados;
        if (base <= 0) return 0.0;
        return Math.round(pagados * 1000.0 / base) / 10.0;
    }

    // pagos a tiempo sobre los vencidos de los últimos 12 meses, sin anulados ni en revisión RN-10
    public static Cumplimiento cumplimiento(List<Cuota> cuotas, LocalDate hoy) {
        LocalDate inicio = hoy.minusMonths(12);
        List<Cuota> elegibles = cuotas.stream()
                .filter(c -> !c.vencimiento().isBefore(inicio))
                .filter(c -> c.vencimiento().isBefore(hoy) || (c.vencimiento().equals(hoy) && c.fechaPago() != null))
                .filter(c -> c.estado() != EstadoCompromiso.ANULADO && c.estado() != EstadoCompromiso.EN_REVISION)
                .toList();
        int aTiempo = (int) elegibles.stream()
                .filter(c -> c.fechaPago() != null && !c.fechaPago().isAfter(c.vencimiento()))
                .count();

        // 1. con menos de tres no se clasifica
        if (elegibles.size() < 3) return new Cumplimiento(elegibles.size(), aTiempo, null, null);

        // 2. categoría con el porcentaje exacto; se informa con un decimal
        double porcentaje = aTiempo * 100.0 / elegibles.size();
        String categoria = porcentaje >= 95.0 ? "EJEMPLAR" : porcentaje >= 80.0 ? "AL_DIA" : "EN_CAMINO";
        return new Cumplimiento(elegibles.size(), aTiempo, Math.round(porcentaje * 10.0) / 10.0, categoria);
    }

    // --- helpers privados ---
    private static boolean cuotaAnteriorDelPlan(Cuota c, Cuota objetivo) {
        return c.numeroCuota() != null && objetivo.numeroCuota() != null
                && Objects.equals(c.totalCuotas(), objetivo.totalCuotas())
                && normalizar(c.concepto()).equals(normalizar(objetivo.concepto()))
                && c.numeroCuota() < objetivo.numeroCuota();
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase(Locale.ROOT);
    }

    private static int mayorCorrelativo(Collection<String> valores, String prefijo) {
        return valores.stream()
                .filter(f -> f.startsWith(prefijo))
                .map(f -> f.substring(prefijo.length()))
                .filter(n -> !n.isEmpty() && n.chars().allMatch(Character::isDigit))
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(0);
    }
}
