package com.pagoflex.soporte.features.reporte.service;

import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.rules.ReglasReporte;

import java.time.LocalDate;

public final class ReporteMapper {
    private ReporteMapper() {
    }

    public static boolean atrasado(Reporte r, LocalDate hoy) {
        return ReglasReporte.estaAtrasado(r.getEstado(), r.getFechaCreacion().toLocalDate(), hoy);
    }

    public static ReporteResponse toResponse(Reporte r, LocalDate hoy) {
        return new ReporteResponse(
                r.getNumero(),
                r.getFolio(),
                r.getMotivo(),
                r.getComentario(),
                r.getEstado(),
                atrasado(r, hoy),
                r.getFechaCreacion(),
                new ReporteResponse.CobroResponse(r.getEmpresa(), r.getConcepto(), r.getNumeroCuota(),
                        r.getTotalCuotas(), r.getTotal(), r.getFechaVencimiento(), r.getEstadoCompromiso()),
                new ReporteResponse.UsuarioResponse(r.getNombreUsuario(), r.getUsuarioCorreo(), r.getTelefonoUsuario()),
                r.getAgenteCorreo(),
                r.getFechaToma(),
                r.getFechaAtencion(),
                r.getRespuesta(),
                r.getFechaResolucion(),
                r.getHistorial().stream()
                        .map(h -> new ReporteResponse.CambioEstadoResponse(h.getEstadoAnterior(), h.getEstadoNuevo(),
                                h.getActorCorreo(), h.getFecha()))
                        .toList(),
                r.getComprobante() != null
        );
    }
}
