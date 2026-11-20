package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ComprobanteResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.MiCompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PagoResponse;
import com.pagoflex.cobranza.features.compromiso.entity.Anulacion;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Comprobante;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Pago;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.cobranza.core.client.PersonaInterna;

import java.time.LocalDate;

/// Convierte los compromisos guardados aplicando el estado y recargo a la fecha
public final class CompromisoMapper {
    private CompromisoMapper() {
    }

    public static EstadoCompromiso estado(Compromiso c, LocalDate hoy) {
        return ReglasCompromiso.estadoEfectivo(c.getEstado(), c.getFechaVencimiento(), hoy);
    }

    public static long recargo(Compromiso c, LocalDate hoy) {
        return ReglasCompromiso.recargoEfectivo(estado(c, hoy), c.getRecargoAplicado(), c.getEmpresa().getRecargoAtraso());
    }

    public static ReglasCompromiso.Cuota cuota(Compromiso c, LocalDate hoy) {
        Pago pago = c.getPago();
        return new ReglasCompromiso.Cuota(c.getFolio(), c.getConcepto(), c.getNumeroCuota(), c.getTotalCuotas(),
                c.getFechaVencimiento(), estado(c, hoy), pago == null ? null : pago.getFechaHora().toLocalDate());
    }

    // ---------- ejecutivo ----------
    public static CompromisoResponse toResponse(Compromiso c, LocalDate hoy, PersonaInterna contacto) {
        // 1. reflejar vencimientos y recargos que aún no se guardaron
        EstadoCompromiso estado = estado(c, hoy);
        long recargo = recargo(c, hoy);
        Anulacion a = c.getAnulacion();

        // 2. armar la respuesta sin datos ajenos a la empresa; el contacto solo en el detalle
        return new CompromisoResponse(
                c.getFolio(),
                c.getEmpresa().getNombreFantasia(),
                c.getConcepto(),
                c.getNumeroCuota(),
                c.getTotalCuotas(),
                c.getMonto(),
                recargo,
                c.getMonto() + recargo,
                c.getFechaVencimiento(),
                estado,
                atraso(c, estado, hoy),
                new CompromisoResponse.PersonaResponse(c.getPersonaNombre(), c.getPersonaRut(),
                        contacto == null ? null : contacto.correo(),
                        contacto == null ? null : contacto.telefono()),
                c.getRegistradoPorCorreo(),
                c.getFechaRegistro(),
                a == null ? null : new CompromisoResponse.AnulacionResponse(a.getMotivo(), a.getComentario(),
                        a.getEjecutivoCorreo(), a.getFecha()),
                numeroComprobante(c)
        );
    }

    // ---------- usuario final ----------
    public static MiCompromisoResponse toMiCompromiso(Compromiso c, LocalDate hoy, String folioAnterior) {
        EstadoCompromiso estado = estado(c, hoy);
        long recargo = recargo(c, hoy);
        Pago pago = c.getPago();
        return new MiCompromisoResponse(
                c.getFolio(),
                c.getEmpresa().getCodigo(),
                c.getEmpresa().getNombreFantasia(),
                c.getEmpresa().getCorreoAtencion(),
                c.getConcepto(),
                c.getNumeroCuota(),
                c.getTotalCuotas(),
                c.getMonto(),
                recargo,
                c.getMonto() + recargo,
                c.getFechaVencimiento(),
                estado,
                atraso(c, estado, hoy),
                ReglasCompromiso.puedePagarse(estado) && folioAnterior == null,
                folioAnterior,
                numeroComprobante(c),
                pago == null ? null : pago.getFechaHora()
        );
    }

    public static PagoResponse toPago(Pago p) {
        Compromiso c = p.getCompromiso();
        return new PagoResponse(
                p.getComprobante().getNumero(),
                c.getFolio(),
                c.getEmpresa().getNombreFantasia(),
                c.getConcepto(),
                c.getNumeroCuota(),
                c.getTotalCuotas(),
                p.getMontoPagado(),
                p.getFechaHora(),
                p.getCanal(),
                p.getMedio(),
                EstadoCompromiso.PAGADO.name()
        );
    }

    public static ComprobanteResponse toComprobante(Comprobante cp) {
        return new ComprobanteResponse(cp.getNumero(), cp.getFolio(), cp.getEmpresa(), cp.getConcepto(),
                cp.getNumeroCuota(), cp.getTotalCuotas(), cp.getPersonaNombre(), cp.getPersonaRut(),
                cp.getFechaVencimiento(), cp.getMonto(), cp.getRecargo(), cp.getTotal(), cp.getCanal(),
                cp.getMedio(), cp.getFechaPago());
    }

    // ---------- otros servicios ----------
    public static CompromisoInterno toInterno(Compromiso c, LocalDate hoy, PersonaInterna contacto, Boolean delUsuario) {
        EstadoCompromiso estado = estado(c, hoy);
        return new CompromisoInterno(
                c.getFolio(),
                c.getPersonaId(),
                c.getEmpresa().getId(),
                c.getEmpresa().getNombreFantasia(),
                c.getConcepto(),
                c.getNumeroCuota(),
                c.getTotalCuotas(),
                c.getMonto() + recargo(c, hoy),
                c.getFechaVencimiento(),
                estado.name(),
                c.getPersonaRut(),
                c.getPersonaNombre(),
                contacto == null ? null : contacto.correo(),
                contacto == null ? null : contacto.telefono(),
                estado == EstadoCompromiso.PENDIENTE || estado == EstadoCompromiso.VENCIDO,
                delUsuario
        );
    }

    // --- helpers privados ---
    private static long atraso(Compromiso c, EstadoCompromiso estado, LocalDate hoy) {
        return estado == EstadoCompromiso.VENCIDO ? ReglasCompromiso.diasDeAtraso(c.getFechaVencimiento(), hoy) : 0;
    }

    private static String numeroComprobante(Compromiso c) {
        Pago pago = c.getPago();
        return pago == null || pago.getComprobante() == null ? null : pago.getComprobante().getNumero();
    }
}
