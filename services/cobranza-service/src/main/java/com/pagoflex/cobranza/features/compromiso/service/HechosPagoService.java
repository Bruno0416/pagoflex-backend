package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.core.client.PersonaInterna;
import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.Pago;
import com.pagoflex.cobranza.features.compromiso.exceptions.CuentaSinPersona;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.contracts.cobranza.CommitmentFact;
import com.pagoflex.contracts.cobranza.CommitmentStatus;
import com.pagoflex.contracts.cobranza.PaymentFact;
import com.pagoflex.contracts.cobranza.PaymentFactsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HechosPagoService {
    private final CompromisoRepository compromisoRepo;
    private final UsuariosServiceClient usuarios;
    private final Clock clock;

    // cobranza es la fuente de verdad de los pagos; reconocimientos solo los lee
    @Transactional(readOnly = true)
    public PaymentFactsResponse deUsuario(UUID usuarioId) {
        // 1. persona de la cuenta; sin persona no hay historial (404 = historial vacío)
        PersonaInterna persona = usuarios.porUsuario(usuarioId).orElseThrow(CuentaSinPersona::new);
        LocalDate hoy = LocalDate.now(clock);

        // 2. cada compromiso con su estado a la fecha y su pago si existe
        var hechos = compromisoRepo.findDePersona(persona.id()).stream()
                .map(c -> hecho(c, hoy))
                .toList();
        return new PaymentFactsResponse(usuarioId, OffsetDateTime.now(clock), hechos);
    }

    // --- helper privado ---
    private CommitmentFact hecho(Compromiso c, LocalDate hoy) {
        Pago pago = c.getPago();
        PaymentFact hechoPago = pago == null || pago.getComprobante() == null ? null
                : new PaymentFact(pago.getComprobante().getNumero(),
                pago.getFechaHora().atZone(clock.getZone()).toOffsetDateTime());
        return new CommitmentFact(c.getId(), c.getFolio(), c.getFechaVencimiento(),
                CommitmentStatus.valueOf(CompromisoMapper.estado(c, hoy).name()), hechoPago);
    }
}
