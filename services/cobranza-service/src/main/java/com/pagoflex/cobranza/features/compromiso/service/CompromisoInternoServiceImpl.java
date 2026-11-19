package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.exceptions.CambioEstadoNoPermitido;
import com.pagoflex.cobranza.features.compromiso.exceptions.CompromisoNoEncontrado;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompromisoInternoServiceImpl implements CompromisoInternoService {
    private final CompromisoRepository compromisoRepo;
    private final UsuariosServiceClient usuarios;
    private final Clock clock;

    // soporte pregunta si el cobro existe, de quién es y si se puede reportar
    @Override
    @Transactional(readOnly = true)
    public CompromisoInterno reportabilidad(String folio, UUID usuarioId) {
        Compromiso compromiso = compromisoRepo.findByFolio(folio).orElseThrow(() -> new CompromisoNoEncontrado(folio));

        // 1. contacto de la persona para el agente; si usuarios no responde va sin él
        PersonaInterna persona;
        try {
            persona = usuarios.porId(compromiso.getPersonaId()).orElse(null);
        } catch (RuntimeException ex) {
            persona = null;
        }

        // 2. dueño: la cuenta del token tiene que ser la de la persona del cobro
        Boolean delUsuario = usuarioId == null ? null
                : persona != null && usuarioId.equals(persona.usuarioId());
        return CompromisoMapper.toInterno(compromiso, LocalDate.now(clock), persona, delUsuario);
    }

    // al reportar un problema el cobro queda en revisión y no se puede pagar RN-09
    @Override
    @Transactional
    public CompromisoInterno marcarEnRevision(String folio) {
        Compromiso compromiso = buscarParaActualizar(folio);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. repetir la llamada no cambia nada
        EstadoCompromiso estado = CompromisoMapper.estado(compromiso, hoy);
        if (estado == EstadoCompromiso.EN_REVISION) return CompromisoMapper.toInterno(compromiso, hoy, null, null);

        // 2. solo pendientes o vencidos pueden pasar a revisión
        if (estado != EstadoCompromiso.PENDIENTE && estado != EstadoCompromiso.VENCIDO) {
            throw new CambioEstadoNoPermitido("Este cobro no se puede reportar porque está " + estado.name());
        }

        // 3. si ya venció, guardar el recargo antes de pasar a revisión RN-03
        compromiso.setRecargoAplicado(CompromisoMapper.recargo(compromiso, hoy));
        compromiso.setEstado(EstadoCompromiso.EN_REVISION);
        compromiso.setFechaCambioEstado(ahora);
        return CompromisoMapper.toInterno(compromiso, hoy, null, null);
    }

    // al resolver el reporte el cobro vuelve según su fecha; si se anuló, queda anulado
    @Override
    @Transactional
    public CompromisoInterno salirDeRevision(String folio) {
        Compromiso compromiso = buscarParaActualizar(folio);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. solo cambia si sigue en revisión; repetir la llamada no hace nada
        if (compromiso.getEstado() == EstadoCompromiso.EN_REVISION) {
            EstadoCompromiso nuevo = ReglasCompromiso.estadoSegunFecha(compromiso.getFechaVencimiento(), hoy);
            compromiso.setRecargoAplicado(ReglasCompromiso.recargoEfectivo(
                    nuevo, compromiso.getRecargoAplicado(), compromiso.getEmpresa().getRecargoAtraso()));
            compromiso.setEstado(nuevo);
            compromiso.setFechaCambioEstado(ahora);
        }
        return CompromisoMapper.toInterno(compromiso, hoy, null, null);
    }

    // --- helper privado ---
    private Compromiso buscarParaActualizar(String folio) {
        return compromisoRepo.findByFolioParaActualizar(folio).orElseThrow(() -> new CompromisoNoEncontrado(folio));
    }
}
