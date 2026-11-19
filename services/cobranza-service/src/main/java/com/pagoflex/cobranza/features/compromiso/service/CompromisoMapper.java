package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.entity.Anulacion;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Persona;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.contracts.cobranza.CompromisoInterno;

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

    public static CompromisoResponse toResponse(Compromiso c, LocalDate hoy) {
        // 1. reflejar vencimientos y recargos que aún no se guardaron
        EstadoCompromiso estado = estado(c, hoy);
        long recargo = recargo(c, hoy);
        Persona p = c.getPersona();
        Anulacion a = c.getAnulacion();

        // 2. armar la respuesta sin datos ajenos a la empresa
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
                estado == EstadoCompromiso.VENCIDO ? ReglasCompromiso.diasDeAtraso(c.getFechaVencimiento(), hoy) : 0,
                new CompromisoResponse.PersonaResponse(p.nombreCompleto(), p.getRut(), p.getCorreo(), p.getTelefono()),
                c.getRegistradoPor(),
                c.getFechaRegistro(),
                a == null ? null : new CompromisoResponse.AnulacionResponse(a.getMotivo(), a.getComentario(),
                        a.getEjecutivo(), a.getFecha())
        );
    }

    public static CompromisoInterno toInterno(Compromiso c, LocalDate hoy) {
        Persona p = c.getPersona();
        return new CompromisoInterno(
                c.getFolio(),
                c.getEmpresa().getNombreFantasia(),
                c.getConcepto(),
                c.getNumeroCuota(),
                c.getTotalCuotas(),
                c.getMonto() + recargo(c, hoy),
                c.getFechaVencimiento(),
                estado(c, hoy).name(),
                p.nombreCompleto(),
                p.getCorreo(),
                p.getTelefono()
        );
    }
}
