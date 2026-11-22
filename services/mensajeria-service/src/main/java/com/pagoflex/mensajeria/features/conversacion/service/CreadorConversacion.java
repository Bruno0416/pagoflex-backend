package com.pagoflex.mensajeria.features.conversacion.service;

import com.pagoflex.mensajeria.features.conversacion.entity.ConversacionTicket;
import com.pagoflex.mensajeria.features.conversacion.repository.ConversacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

/// Crea la conversación la primera vez que alguien la abre, en su propia transacción
@Component
@RequiredArgsConstructor
public class CreadorConversacion {
    private final ConversacionRepository repo;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void asegurar(String numero, UUID empresaId) {
        if (repo.findByReporteNumero(numero).isPresent()) return;
        try {
            repo.saveAndFlush(ConversacionTicket.builder()
                    .reporteNumero(numero)
                    .empresaId(empresaId)
                    .fechaCreacion(LocalDateTime.now(clock))
                    .ultimaSecuencia(0)
                    .build());
        } catch (DataIntegrityViolationException ex) {
            // otro participante la creó al mismo tiempo: sirve la suya
        }
    }
}
