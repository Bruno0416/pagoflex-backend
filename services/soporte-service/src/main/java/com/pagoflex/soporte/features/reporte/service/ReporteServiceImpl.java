package com.pagoflex.soporte.features.reporte.service;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.security.AuthenticatedUser;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.exceptions.*;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import com.pagoflex.soporte.features.reporte.rules.ReglasReporte;
import com.pagoflex.core.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {
    private static final Set<EstadoReporte> SIN_RESOLVER = Set.of(EstadoReporte.ABIERTO, EstadoReporte.EN_GESTION);

    private final ReporteRepository repository;
    private final CobranzaServiceClient cobranza;
    private final Clock clock;

    @Override
    @Transactional
    public ReporteResponse reportar(AuthenticatedUser usuario, ReportarRequest request) {
        String folio = request.folio().trim().toUpperCase();
        String comentario = request.comentario() == null || request.comentario().isBlank()
                ? null : request.comentario().trim();
        if (request.motivo() == MotivoReporte.OTRO && comentario == null) {
            throw new ComentarioObligatorio();
        }

        // el cobro tiene que ser del usuario: lo comparo por correo con la persona de cobranza
        CompromisoInterno cobro = cobranza.obtener(folio);
        if (usuario.email() == null || !usuario.email().equalsIgnoreCase(cobro.correoPersona())) {
            throw new CompromisoNoEncontrado(folio);
        }

        repository.findFirstByFolioAndEstadoIn(folio, SIN_RESOLVER)
                .ifPresent(r -> {
                    throw new ReporteYaExiste(r.getNumero());
                });

        // cobranza lo deja EN_REVISION RN-09; si no se puede responde 409
        CompromisoInterno enRevision = cobranza.marcarEnRevision(folio);

        Reporte reporte = Reporte.builder()
                .numero(ReglasReporte.siguienteNumero(repository.findNumeros()))
                .folio(folio)
                .motivo(request.motivo())
                .comentario(comentario)
                .estado(EstadoReporte.ABIERTO)
                .fechaCreacion(LocalDateTime.now(clock))
                .empresa(enRevision.empresa())
                .concepto(enRevision.concepto())
                .numeroCuota(enRevision.numeroCuota())
                .totalCuotas(enRevision.totalCuotas())
                .estadoCompromiso(enRevision.estado())
                .total(enRevision.total())
                .fechaVencimiento(enRevision.fechaVencimiento())
                .nombreUsuario(enRevision.nombrePersona())
                .correoUsuario(enRevision.correoPersona())
                .telefonoUsuario(enRevision.telefonoPersona())
                .build();
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> misReportes(String correo) {
        if (correo == null) return List.of();
        LocalDate hoy = hoy();
        return repository.findByCorreoUsuarioIgnoreCaseOrderByFechaCreacionDesc(correo).stream()
                .map(r -> ReporteMapper.toResponse(r, hoy))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReporteResponse> bandeja(EstadoReporte estado) {
        LocalDate hoy = hoy();
        // primero los atrasados y después por antigüedad
        return repository.findAllByOrderByFechaCreacionAsc().stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .sorted(Comparator.comparing((Reporte r) -> !ReporteMapper.atrasado(r, hoy))
                        .thenComparing(Reporte::getFechaCreacion))
                .map(r -> ReporteMapper.toResponse(r, hoy))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse detalle(String numero) {
        return ReporteMapper.toResponse(buscar(numero), hoy());
    }

    @Override
    @Transactional
    public ReporteResponse tomar(String agente, String numero) {
        Reporte reporte = buscar(numero);
        if (reporte.getEstado() != EstadoReporte.ABIERTO) {
            throw new ReporteYaTomado();
        }
        reporte.setEstado(EstadoReporte.EN_GESTION);
        reporte.setAgente(EmailUtils.normalize(agente));
        reporte.setFechaToma(LocalDateTime.now(clock));
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    @Override
    @Transactional
    public ReporteResponse resolver(String agente, String numero, ResolverRequest request) {
        Reporte reporte = buscar(numero);
        if (reporte.getEstado() != EstadoReporte.EN_GESTION) {
            throw new ReporteSinTomar();
        }
        if (agente == null || !EmailUtils.normalize(agente).equals(reporte.getAgente())) {
            throw new ReporteDeOtroAgente();
        }
        if (request.respuesta() == null || request.respuesta().isBlank()) {
            throw new RespuestaObligatoria();
        }

        // el cobro vuelve a su estado según la fecha; si ya se pagó o anuló cobranza no lo toca
        CompromisoInterno cobro = cobranza.salirDeRevision(reporte.getFolio());
        reporte.setEstadoCompromiso(cobro.estado());

        reporte.setEstado(EstadoReporte.RESUELTO);
        reporte.setRespuesta(request.respuesta().trim());
        reporte.setFechaResolucion(LocalDateTime.now(clock));
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    private Reporte buscar(String numero) {
        return repository.findByNumero(numero.trim().toUpperCase())
                .orElseThrow(() -> new ReporteNoEncontrado(numero));
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }
}
