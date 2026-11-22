package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Pago;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.contracts.cobranza.HechoCompromiso;
import com.pagoflex.contracts.cobranza.EstadoCompromisoEvaluacion;
import com.pagoflex.contracts.cobranza.HechoPago;
import com.pagoflex.contracts.cobranza.RespuestaHechosPago;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.core.error.PagoFlexException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/// Hechos de pago que lee reconocimientos-service; usa el contrato de libs/contracts sin inventar estados ni fechas.
@Service
@RequiredArgsConstructor
public class HechosPagoService {
    private final CompromisoRepository compromisoRepo;
    private final UsuariosServiceClient usuarios;
    private final Clock clock;

    @Transactional(readOnly = true)
    public RespuestaHechosPago hechos(UUID usuarioId) {
        // 1. la persona de la cuenta; sin persona no hay historial que informar
        PersonaInterna persona = usuarios.porUsuario(usuarioId).orElseThrow(
                () -> new PagoFlexException(404, "La cuenta no tiene una persona asociada"));

        // 2. cada compromiso con su estado efectivo a hoy y, si está pagado, su comprobante y fecha reales
        LocalDate hoy = LocalDate.now(clock);
        List<HechoCompromiso> compromisos = compromisoRepo.findDePersona(persona.id()).stream()
                .map(c -> hecho(c, hoy))
                .toList();
        return new RespuestaHechosPago(usuarioId, OffsetDateTime.now(clock), compromisos);
    }

    // --- helper privado ---
    private HechoCompromiso hecho(Compromiso c, LocalDate hoy) {
        EstadoCompromiso estado = CompromisoMapper.estado(c, hoy);
        Pago pago = estado == EstadoCompromiso.PAGADO ? c.getPago() : null;
        HechoPago pagoHecho = pago == null ? null : new HechoPago(
                pago.getComprobante().getNumero(),
                pago.getFechaHora().atZone(clock.getZone()).toOffsetDateTime());
        return new HechoCompromiso(c.getId(), c.getFolio(), c.getFechaVencimiento(),
                EstadoCompromisoEvaluacion.valueOf(estado.name()), pagoHecho);
    }
}
