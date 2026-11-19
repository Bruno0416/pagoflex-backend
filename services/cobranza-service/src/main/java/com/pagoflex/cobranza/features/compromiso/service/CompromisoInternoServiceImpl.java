package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.exceptions.CambioEstadoNoPermitido;
import com.pagoflex.cobranza.features.compromiso.exceptions.CompromisoNoEncontrado;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.contracts.cobranza.CompromisoInterno;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CompromisoInternoServiceImpl implements CompromisoInternoService {
    private final CompromisoRepository compromisoRepo;
    private final Clock clock;

    @Override
    public CompromisoInterno obtener(String folio) {
        return CompromisoMapper.toInterno(buscar(folio), LocalDate.now(clock));
    }

    // al reportar un problema el cobro queda en revisión y no se puede pagar RN-09
    @Override
    @Transactional
    public CompromisoInterno marcarEnRevision(String folio) {
        Compromiso compromiso = buscar(folio);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. solo pendientes o vencidos pueden pasar a revisión
        EstadoCompromiso estado = CompromisoMapper.estado(compromiso, hoy);
        if (estado != EstadoCompromiso.PENDIENTE && estado != EstadoCompromiso.VENCIDO) {
            throw new CambioEstadoNoPermitido("Este cobro no se puede reportar porque está " + estado.name());
        }

        // 2. si ya venció, guardar el recargo antes de pasar a revisión RN-03
        compromiso.setRecargoAplicado(CompromisoMapper.recargo(compromiso, hoy));
        compromiso.setEstado(EstadoCompromiso.EN_REVISION);
        compromiso.setFechaCambioEstado(ahora);
        return CompromisoMapper.toInterno(compromiso, hoy);
    }

    // al resolver el reporte el cobro vuelve según su fecha; si se anuló, queda anulado
    @Override
    @Transactional
    public CompromisoInterno salirDeRevision(String folio) {
        Compromiso compromiso = buscar(folio);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. solo cambia si sigue en revisión
        if (compromiso.getEstado() == EstadoCompromiso.EN_REVISION) {
            EstadoCompromiso nuevo = ReglasCompromiso.estadoSegunFecha(compromiso.getFechaVencimiento(), hoy);
            compromiso.setRecargoAplicado(ReglasCompromiso.recargoEfectivo(
                    nuevo, compromiso.getRecargoAplicado(), compromiso.getEmpresa().getRecargoAtraso()));
            compromiso.setEstado(nuevo);
            compromiso.setFechaCambioEstado(ahora);
        }
        return CompromisoMapper.toInterno(compromiso, hoy);
    }

    // --- helper privado ---
    private Compromiso buscar(String folio) {
        return compromisoRepo.findByFolio(folio).orElseThrow(() -> new CompromisoNoEncontrado(folio));
    }
}
