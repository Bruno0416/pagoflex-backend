package com.pagoflex.soporte.features.reporte.service;

import com.pagoflex.contracts.cobranza.CompromisoInterno;
import com.pagoflex.contracts.cobranza.EmpresaInterna;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.security.AuthenticatedUser;
import com.pagoflex.soporte.core.client.CobranzaServiceClient;
import com.pagoflex.soporte.core.exceptions.CobroNoReportable;
import com.pagoflex.soporte.core.exceptions.CompromisoNoEncontrado;
import com.pagoflex.soporte.features.reporte.dto.request.ReportarRequest;
import com.pagoflex.soporte.features.reporte.dto.request.ResolverRequest;
import com.pagoflex.soporte.features.reporte.dto.response.PaginaResponse;
import com.pagoflex.soporte.features.reporte.dto.response.ParticipantesResponse;
import com.pagoflex.soporte.features.reporte.dto.response.ReporteResponse;
import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;
import com.pagoflex.soporte.features.reporte.entity.Reporte;
import com.pagoflex.soporte.features.reporte.exceptions.*;
import com.pagoflex.soporte.features.reporte.repository.ReporteRepository;
import com.pagoflex.soporte.features.reporte.rules.ReglasReporte;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {
    private static final int TAMANO_POR_DEFECTO = 20;
    private static final int TAMANO_MAXIMO = 50;

    private final ReporteRepository repository;
    private final CobranzaServiceClient cobranza;
    private final Clock clock;

    // ---------- usuario final ----------
    @Override
    @Transactional
    public ReporteResponse reportar(AuthenticatedUser usuario, ReportarRequest request) {
        String folio = request.folio().trim().toUpperCase();
        String comentario = request.comentario() == null || request.comentario().isBlank()
                ? null : request.comentario().trim();
        if (request.motivo() == MotivoReporte.OTRO && comentario == null) {
            throw new ComentarioObligatorio();
        }

        // 1. el cobro tiene que ser del usuario; si es ajeno respondo como si no existiera
        CompromisoInterno cobro = cobranza.reportabilidad(folio, usuario.id());
        if (!Boolean.TRUE.equals(cobro.delUsuario())) {
            throw new CompromisoNoEncontrado(folio);
        }

        // 2. un solo reporte activo por cobro
        repository.findFirstByFolioAndEstadoIn(folio, ReglasReporte.ACTIVOS)
                .ifPresent(r -> {
                    throw new ReporteYaExiste(r.getNumero());
                });
        if (!cobro.reportable()) {
            throw new CobroNoReportable("Este cobro está " + cobro.estado() + " y no se puede reportar");
        }

        // 3. cobranza lo deja EN_REVISION RN-09; si no se puede responde 409
        CompromisoInterno enRevision = cobranza.marcarEnRevision(folio);

        LocalDateTime ahora = LocalDateTime.now(clock);
        Reporte reporte = Reporte.builder()
                .numero(ReglasReporte.siguienteNumero(repository.findNumeros()))
                .folio(folio)
                .motivo(request.motivo())
                .comentario(comentario)
                .fechaCreacion(ahora)
                .usuarioId(usuario.id())
                .usuarioCorreo(usuario.email() != null ? EmailUtils.normalize(usuario.email()) : enRevision.correoPersona())
                .build();
        copiarCobro(reporte, enRevision);
        reporte.registrarCambio(EstadoReporte.ABIERTO, usuario.id(), reporte.getUsuarioCorreo(), ahora);
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReporteResponse> misReportes(UUID usuarioId, int page, int size) {
        LocalDate hoy = hoy();
        return PaginaResponse.de(repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId, pagina(page, size)),
                r -> ReporteMapper.toResponse(r, hoy));
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse miReporte(UUID usuarioId, String numero) {
        Reporte reporte = buscar(numero);
        if (!reporte.getUsuarioId().equals(usuarioId)) {
            throw new ReporteNoEncontrado(numero);
        }
        return ReporteMapper.toResponse(reporte, hoy());
    }

    // ---------- agente de soporte ----------
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReporteResponse> bandeja(UUID agenteId, EstadoReporte estado, Boolean atrasado,
                                                   int page, int size) {
        LocalDate hoy = hoy();
        // atrasado = abierto y creado antes del día siguiente al último día que ya cuenta como atrasado
        LocalDateTime limite = ReglasReporte.ultimoDiaAtrasado(hoy).plusDays(1).atStartOfDay();
        return PaginaResponse.de(
                repository.bandeja(agenteId, estado, atrasado, limite, EstadoReporte.ABIERTO, pagina(page, size)),
                r -> ReporteMapper.toResponse(r, hoy));
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse detalle(UUID agenteId, String numero) {
        // el agente ve los abiertos y los suyos; los de otro agente no se muestran
        Reporte reporte = buscar(numero);
        if (reporte.getEstado() != EstadoReporte.ABIERTO && !agenteId.equals(reporte.getAgenteId())) {
            throw new ReporteNoEncontrado(numero);
        }
        return ReporteMapper.toResponse(reporte, hoy());
    }

    @Override
    @Transactional
    public ReporteResponse asignar(AuthenticatedUser agente, String numero) {
        Reporte reporte = buscar(numero);
        if (reporte.getEstado() == EstadoReporte.ASIGNADO || reporte.getEstado() == EstadoReporte.EN_ATENCION) {
            throw new ReporteYaTomado();
        }
        validarPaso(reporte, EstadoReporte.ASIGNADO);

        LocalDateTime ahora = LocalDateTime.now(clock);
        reporte.setAgenteId(agente.id());
        reporte.setAgenteCorreo(correo(agente));
        reporte.setFechaToma(ahora);
        reporte.registrarCambio(EstadoReporte.ASIGNADO, agente.id(), correo(agente), ahora);
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    @Override
    @Transactional
    public ReporteResponse atender(AuthenticatedUser agente, String numero) {
        Reporte reporte = delAgente(agente, numero);
        validarPaso(reporte, EstadoReporte.EN_ATENCION);

        LocalDateTime ahora = LocalDateTime.now(clock);
        reporte.setFechaAtencion(ahora);
        reporte.registrarCambio(EstadoReporte.EN_ATENCION, agente.id(), correo(agente), ahora);
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    @Override
    @Transactional
    public ReporteResponse resolver(AuthenticatedUser agente, String numero, ResolverRequest request) {
        Reporte reporte = delAgente(agente, numero);
        validarPaso(reporte, EstadoReporte.RESUELTO);
        if (request.respuesta() == null || request.respuesta().isBlank()) {
            throw new RespuestaObligatoria();
        }

        // 1. el cobro vuelve a su estado; si cobranza falla el reporte sigue en atención
        CompromisoInterno cobro = cobranza.finRevision(reporte.getFolio());
        reporte.setEstadoCompromiso(cobro.estado());

        // 2. cerrar el reporte con la respuesta para el usuario
        LocalDateTime ahora = LocalDateTime.now(clock);
        reporte.setRespuesta(request.respuesta().trim());
        reporte.setFechaResolucion(ahora);
        reporte.registrarCambio(EstadoReporte.RESUELTO, agente.id(), correo(agente), ahora);
        return ReporteMapper.toResponse(repository.save(reporte), hoy());
    }

    // ---------- ejecutivo de la empresa ----------
    @Override
    @Transactional(readOnly = true)
    public PaginaResponse<ReporteResponse> deMiEmpresa(UUID ejecutivoId, EstadoReporte estado, int page, int size) {
        EmpresaInterna empresa = cobranza.empresaDeEjecutivo(ejecutivoId);
        LocalDate hoy = hoy();
        return PaginaResponse.de(repository.deEmpresa(empresa.id(), estado, pagina(page, size)),
                r -> ReporteMapper.toResponse(r, hoy));
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteResponse reporteActivo(UUID ejecutivoId, String folio) {
        EmpresaInterna empresa = cobranza.empresaDeEjecutivo(ejecutivoId);
        String normalizado = folio.trim().toUpperCase();
        return repository.findFirstByFolioAndEstadoIn(normalizado, ReglasReporte.ACTIVOS)
                .filter(r -> r.getEmpresaId().equals(empresa.id()))
                .map(r -> ReporteMapper.toResponse(r, hoy()))
                .orElseThrow(() -> new SinReporteActivo(normalizado));
    }

    // ---------- interno ----------
    @Override
    @Transactional(readOnly = true)
    public ParticipantesResponse participantes(String numero) {
        Reporte r = buscar(numero);
        return new ParticipantesResponse(r.getNumero(), r.getFolio(), r.getEstado(),
                r.getEmpresaId(), r.getAgenteId(), r.getUsuarioId());
    }

    // --- helpers privados ---
    private Reporte delAgente(AuthenticatedUser agente, String numero) {
        // 1. sin tomar no se puede avanzar; tomado por otro es prohibido
        Reporte reporte = buscar(numero);
        if (reporte.getEstado() == EstadoReporte.ABIERTO) {
            throw new ReporteSinTomar();
        }
        if (!agente.id().equals(reporte.getAgenteId())) {
            throw new ReporteDeOtroAgente();
        }
        return reporte;
    }

    private void validarPaso(Reporte reporte, EstadoReporte hacia) {
        if (!ReglasReporte.transicionValida(reporte.getEstado(), hacia)) {
            throw new TransicionNoPermitida(reporte.getEstado(), hacia);
        }
    }

    private void copiarCobro(Reporte reporte, CompromisoInterno cobro) {
        reporte.setEmpresaId(cobro.empresaId());
        reporte.setEmpresa(cobro.empresa());
        reporte.setConcepto(cobro.concepto());
        reporte.setNumeroCuota(cobro.numeroCuota());
        reporte.setTotalCuotas(cobro.totalCuotas());
        reporte.setEstadoCompromiso(cobro.estado());
        reporte.setTotal(cobro.total());
        reporte.setFechaVencimiento(cobro.fechaVencimiento());
        reporte.setNombreUsuario(cobro.nombrePersona());
        reporte.setTelefonoUsuario(cobro.telefonoPersona());
    }

    private Pageable pagina(int page, int size) {
        // tamaño acotado para no devolver todo de una vez
        int tamano = size <= 0 ? TAMANO_POR_DEFECTO : Math.min(size, TAMANO_MAXIMO);
        return PageRequest.of(Math.max(page, 0), tamano);
    }

    private Reporte buscar(String numero) {
        return repository.findByNumero(numero.trim().toUpperCase())
                .orElseThrow(() -> new ReporteNoEncontrado(numero));
    }

    private static String correo(AuthenticatedUser u) {
        return u.email() == null ? null : EmailUtils.normalize(u.email());
    }

    private LocalDate hoy() {
        return LocalDate.now(clock);
    }
}
