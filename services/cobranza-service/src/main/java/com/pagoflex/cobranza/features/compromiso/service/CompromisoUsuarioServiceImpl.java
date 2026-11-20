package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.core.exceptions.ReglaRechazada;
import com.pagoflex.cobranza.features.compromiso.dto.request.PagarRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.*;
import com.pagoflex.cobranza.features.compromiso.entity.*;
import com.pagoflex.cobranza.features.compromiso.exceptions.ComprobanteNoEncontrado;
import com.pagoflex.cobranza.features.compromiso.exceptions.CompromisoNoEncontrado;
import com.pagoflex.cobranza.features.compromiso.exceptions.CuentaSinPersona;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.ComprobanteRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PagoRepository;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.cobranza.core.client.PersonaInterna;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompromisoUsuarioServiceImpl implements CompromisoUsuarioService {
    private static final int CONFLICTO = HttpStatus.CONFLICT.value();

    private final CompromisoRepository compromisoRepo;
    private final PagoRepository pagoRepo;
    private final ComprobanteRepository comprobanteRepo;
    private final AnulacionRepository anulacionRepo;
    private final UsuariosServiceClient usuarios;
    private final Clock clock;

    // ---------- consultas ----------
    @Override
    @Transactional(readOnly = true)
    public List<MiCompromisoResponse> misCompromisos(UUID usuarioId, EstadoCompromiso estado, YearMonth mes) {
        LocalDate hoy = LocalDate.now(clock);
        List<Compromiso> todos = compromisoRepo.findDePersona(persona(usuarioId).id());

        // 1. estado a la fecha y orden de pago calculados sobre todos los suyos
        return todos.stream()
                .filter(c -> mes == null || YearMonth.from(c.getFechaVencimiento()).equals(mes))
                .map(c -> CompromisoMapper.toMiCompromiso(c, hoy, anterior(c, todos, hoy)))
                .filter(c -> estado == null || c.estado() == estado)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ResumenUsuarioResponse resumen(UUID usuarioId, YearMonth mes) {
        LocalDate hoy = LocalDate.now(clock);
        List<Compromiso> todos = compromisoRepo.findDePersona(persona(usuarioId).id());
        List<MiCompromisoResponse> vista = todos.stream()
                .map(c -> CompromisoMapper.toMiCompromiso(c, hoy, anterior(c, todos, hoy)))
                .toList();
        List<MiCompromisoResponse> delMes = vista.stream()
                .filter(c -> YearMonth.from(c.fechaVencimiento()).equals(mes))
                .toList();

        // 1. lo que falta pagar del mes y lo ya pagado
        long porPagar = delMes.stream().filter(c -> ReglasCompromiso.puedePagarse(c.estado()))
                .mapToLong(MiCompromisoResponse::total).sum();
        long pagado = delMes.stream().filter(c -> c.estado() == EstadoCompromiso.PAGADO)
                .mapToLong(MiCompromisoResponse::total).sum();
        int pendientes = (int) delMes.stream().filter(c -> c.estado() == EstadoCompromiso.PENDIENTE).count();

        // 2. vencidos de cualquier mes y los tres próximos vencimientos
        List<MiCompromisoResponse> vencidos = vista.stream()
                .filter(c -> c.estado() == EstadoCompromiso.VENCIDO).toList();
        List<MiCompromisoResponse> proximos = vista.stream()
                .filter(c -> c.estado() == EstadoCompromiso.PENDIENTE)
                .sorted(Comparator.comparing(MiCompromisoResponse::fechaVencimiento))
                .limit(3)
                .toList();

        return new ResumenUsuarioResponse(mes, porPagar, pagado, pendientes, vencidos.size(),
                vencidos.stream().mapToLong(MiCompromisoResponse::total).sum(), proximos);
    }

    @Override
    @Transactional(readOnly = true)
    public MiCompromisoResponse detalle(UUID usuarioId, String folio) {
        Compromiso compromiso = propio(folio, persona(usuarioId).id());
        LocalDate hoy = LocalDate.now(clock);
        return CompromisoMapper.toMiCompromiso(compromiso, hoy, anterior(compromiso, hermanos(compromiso), hoy));
    }

    // ---------- pago simulado RN-05/06/07 ----------
    @Override
    @Transactional(readOnly = true)
    public CotizacionResponse cotizar(UUID usuarioId, String folio) {
        Compromiso compromiso = propio(folio, persona(usuarioId).id());
        LocalDate hoy = LocalDate.now(clock);

        // 1. mismas validaciones que al confirmar, para avisar antes
        validarPagable(compromiso, hoy);
        long recargo = CompromisoMapper.recargo(compromiso, hoy);
        long total = compromiso.getMonto() + recargo;

        return new CotizacionResponse(
                compromiso.getFolio(),
                compromiso.getEmpresa().getNombreFantasia(),
                compromiso.getConcepto(),
                compromiso.getNumeroCuota(),
                compromiso.getTotalCuotas(),
                compromiso.getMonto(),
                recargo,
                total,
                compromiso.getFechaVencimiento(),
                hoy,
                ReglasCompromiso.cotizacion(compromiso.getFolio(), total, hoy));
    }

    @Override
    @Transactional
    public PagoConfirmado pagar(UUID usuarioId, String folio, String llave, PagarRequest request) {
        // 1. la misma llave devuelve el pago ya hecho, sin cobrar dos veces
        String llaveLimpia = validarLlave(llave);
        Optional<Pago> repetido = pagoRepo.findPorLlave(usuarioId, llaveLimpia);
        if (repetido.isPresent()) {
            if (!repetido.get().getCompromiso().getFolio().equals(folio)) {
                throw new ReglaRechazada(CONFLICTO, "IDEMPOTENCY_KEY_REUSED",
                        "Esa llave de pago ya se usó para otro compromiso");
            }
            return new PagoConfirmado(CompromisoMapper.toPago(repetido.get()), false);
        }

        // 2. bloquear el compromiso y revalidar todo dentro de la transacción
        PersonaInterna persona = persona(usuarioId);
        Compromiso compromiso = compromisoRepo.findByFolioParaActualizar(folio)
                .filter(c -> c.getPersonaId().equals(persona.id()))
                .orElseThrow(() -> new CompromisoNoEncontrado(folio));
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();
        validarPagable(compromiso, hoy);

        // 3. el total lo calcula el servidor; si cambió desde la cotización se rechaza
        long recargo = CompromisoMapper.recargo(compromiso, hoy);
        long total = compromiso.getMonto() + recargo;
        if (!ReglasCompromiso.cotizacion(folio, total, hoy).equals(request.cotizacionId().trim())) {
            throw new ReglaRechazada(CONFLICTO, "QUOTE_CHANGED",
                    "El total cambió desde que lo consultaste, revísalo antes de pagar",
                    Map.of("total", total));
        }

        // 4. solo esta cuota queda pagada; la siguiente se habilita sola por orden
        compromiso.setEstado(EstadoCompromiso.PAGADO);
        compromiso.setRecargoAplicado(recargo);
        compromiso.setFechaCambioEstado(ahora);

        // 5. pago único con comprobante fijo
        Pago pago = Pago.builder()
                .compromiso(compromiso)
                .usuarioId(usuarioId)
                .idempotencyKey(llaveLimpia)
                .fechaHora(ahora)
                .montoPagado(total)
                .canal(CanalPago.APP)
                .medio(request.medioPago())
                .build();
        pago.setComprobante(comprobante(pago, compromiso, recargo, total));
        pagoRepo.saveAndFlush(pago);
        compromiso.setPago(pago);

        return new PagoConfirmado(CompromisoMapper.toPago(pago), true);
    }

    // ---------- historial ----------
    @Override
    @Transactional(readOnly = true)
    public List<PagoResponse> pagos(UUID usuarioId, String empresa, LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(clock);
        LocalDate inicio = desde == null ? hoy.minusMonths(12) : desde;
        LocalDate fin = hasta == null ? hoy : hasta;
        if (fin.isBefore(inicio)) {
            throw new ReglaRechazada(HttpStatus.BAD_REQUEST.value(), "INVALID_RANGE",
                    "La fecha final no puede ser anterior a la inicial");
        }
        return pagoRepo.findDePersona(persona(usuarioId).id(),
                        empresa == null || empresa.isBlank() ? null : empresa.trim().toUpperCase(),
                        inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay()).stream()
                .map(CompromisoMapper::toPago)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ComprobanteResponse comprobante(UUID usuarioId, String numero) {
        // 1. solo el dueño ve su comprobante; uno ajeno se trata como inexistente
        UUID personaId = persona(usuarioId).id();
        return comprobanteRepo.findByNumero(numero)
                .filter(cp -> cp.getPago().getCompromiso().getPersonaId().equals(personaId))
                .map(CompromisoMapper::toComprobante)
                .orElseThrow(() -> new ComprobanteNoEncontrado(numero));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnulacionUsuarioResponse> anulaciones(UUID usuarioId) {
        return anulacionRepo.findDePersona(persona(usuarioId).id()).stream()
                .map(a -> new AnulacionUsuarioResponse(
                        a.getCompromiso().getFolio(),
                        a.getCompromiso().getEmpresa().getNombreFantasia(),
                        a.getCompromiso().getConcepto(),
                        a.getCompromiso().getMonto(),
                        a.getMotivo(),
                        a.getComentario(),
                        a.getFecha()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CumplimientoResponse cumplimiento(UUID usuarioId) {
        LocalDate hoy = LocalDate.now(clock);
        List<ReglasCompromiso.Cuota> cuotas = compromisoRepo.findDePersona(persona(usuarioId).id()).stream()
                .map(c -> CompromisoMapper.cuota(c, hoy))
                .toList();
        ReglasCompromiso.Cumplimiento r = ReglasCompromiso.cumplimiento(cuotas, hoy);
        return new CumplimientoResponse(hoy.minusMonths(12), hoy, r.elegibles(), r.pagadosATiempo(),
                r.porcentaje(), r.categoria());
    }

    // --- helpers privados ---
    private PersonaInterna persona(UUID usuarioId) {
        return usuarios.porUsuario(usuarioId).orElseThrow(CuentaSinPersona::new);
    }

    private Compromiso propio(String folio, UUID personaId) {
        // uno de otra persona responde igual que uno inexistente
        return compromisoRepo.findByFolio(folio)
                .filter(c -> c.getPersonaId().equals(personaId))
                .orElseThrow(() -> new CompromisoNoEncontrado(folio));
    }

    private List<Compromiso> hermanos(Compromiso c) {
        return compromisoRepo.findDePersonaEnEmpresa(c.getPersonaId(), c.getEmpresa().getId());
    }

    private String anterior(Compromiso c, List<Compromiso> todos, LocalDate hoy) {
        List<ReglasCompromiso.Cuota> deLaEmpresa = todos.stream()
                .filter(o -> o.getEmpresa().getId().equals(c.getEmpresa().getId()))
                .map(o -> CompromisoMapper.cuota(o, hoy))
                .toList();
        if (!ReglasCompromiso.puedePagarse(CompromisoMapper.estado(c, hoy))) return null;
        return ReglasCompromiso.folioAnteriorPorPagar(CompromisoMapper.cuota(c, hoy), deLaEmpresa).orElse(null);
    }

    private void validarPagable(Compromiso c, LocalDate hoy) {
        // 1. estado que permite pagar RN-08/09
        EstadoCompromiso estado = CompromisoMapper.estado(c, hoy);
        switch (estado) {
            case PAGADO -> throw new ReglaRechazada(CONFLICTO, "ALREADY_PAID", "Este compromiso ya está pagado");
            case EN_REVISION -> throw new ReglaRechazada(CONFLICTO, "IN_REVIEW",
                    "Este compromiso está en revisión por un reporte y no se puede pagar");
            case ANULADO -> throw new ReglaRechazada(CONFLICTO, "CANCELLED", "Este compromiso está anulado");
            default -> {
            }
        }

        // 2. primero la cuota o el vencimiento anterior de la misma empresa RN-04
        String anterior = anterior(c, hermanos(c), hoy);
        if (anterior != null) {
            throw new ReglaRechazada(CONFLICTO, "PAYMENT_OUT_OF_ORDER",
                    "Primero tienes que pagar el compromiso " + anterior,
                    Map.of("folioAnterior", anterior));
        }
    }

    private Comprobante comprobante(Pago pago, Compromiso c, long recargo, long total) {
        return Comprobante.builder()
                .numero(ReglasCompromiso.siguienteComprobante(comprobanteRepo.findNumeros()))
                .pago(pago)
                .folio(c.getFolio())
                .empresa(c.getEmpresa().getNombreFantasia())
                .concepto(c.getConcepto())
                .numeroCuota(c.getNumeroCuota())
                .totalCuotas(c.getTotalCuotas())
                .personaNombre(c.getPersonaNombre())
                .personaRut(c.getPersonaRut())
                .fechaVencimiento(c.getFechaVencimiento())
                .monto(c.getMonto())
                .recargo(recargo)
                .total(total)
                .canal(pago.getCanal().name())
                .medio(pago.getMedio().name())
                .fechaPago(pago.getFechaHora())
                .build();
    }

    private static String validarLlave(String llave) {
        // una llave por intento de pago, como un uuid generado en la app
        if (llave == null || llave.isBlank() || llave.trim().length() > 64) {
            throw new ReglaRechazada(HttpStatus.BAD_REQUEST.value(), "INVALID_IDEMPOTENCY_KEY",
                    "La cabecera Idempotency-Key es obligatoria y de hasta 64 caracteres");
        }
        return llave.trim();
    }
}
