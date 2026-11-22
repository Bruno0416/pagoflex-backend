package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.Anulacion;
import com.pagoflex.cobranza.features.compromiso.entity.Compromiso;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.MotivoAnulacion;
import com.pagoflex.cobranza.features.compromiso.exceptions.AnulacionNoPermitida;
import com.pagoflex.cobranza.features.compromiso.exceptions.CompromisoDuplicado;
import com.pagoflex.cobranza.features.compromiso.exceptions.CompromisoNoEncontrado;
import com.pagoflex.cobranza.features.compromiso.exceptions.DatosCompromisoInvalidos;
import com.pagoflex.cobranza.features.compromiso.exceptions.InvalidRut;
import com.pagoflex.cobranza.features.compromiso.exceptions.PersonaNoEncontrada;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.cobranza.features.empresa.dto.response.EmpresaResponse;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.contracts.usuarios.SolicitudCrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.core.utils.RutUtils;
import com.pagoflex.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CobranzaEjecutivoServiceImpl implements CobranzaEjecutivoService {
    private final CompromisoRepository compromisoRepo;
    private final AnulacionRepository anulacionRepo;
    private final EjecutivoService ejecutivoService;
    private final UsuariosServiceClient usuarios;
    private final Clock clock;

    // ---------- empresa y persona ----------
    @Override
    public EmpresaResponse empresa(UUID ejecutivoId) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivoId);
        return new EmpresaResponse(empresa.getCodigo(), empresa.getNombreFantasia(), empresa.getRecargoAtraso());
    }

    @Override
    public PersonaEncontradaResponse buscarPersona(UUID ejecutivoId, String rut) {
        // 1. solo un ejecutivo con empresa puede buscar
        ejecutivoService.empresaDe(ejecutivoId);

        // 2. la persona vive en usuarios-service
        return usuarios.porRut(normalizarRut(rut))
                .map(p -> new PersonaEncontradaResponse(nombre(p), p.correo()))
                .orElseThrow(PersonaNoEncontrada::new);
    }

    // ---------- registro RN-20 ----------
    @Override
    @Transactional
    public CompromisoResponse registrar(AuthenticatedUser ejecutivo, RegistrarCompromisoRequest request) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo.id());
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. validar rut, fecha y cuotas antes de llamar a usuarios
        String rut = normalizarRut(request.rut());
        if (request.fechaVencimiento().isBefore(hoy)) {
            throw new DatosCompromisoInvalidos("El vencimiento no puede ser anterior a hoy");
        }
        validarCuotas(request.numeroCuota(), request.totalCuotas());

        // 2. usar la persona registrada o crearla sin cuenta con sus datos básicos
        PersonaInterna persona = usuarios.porRut(rut).orElseGet(() -> nuevaPersona(rut, request));

        // 3. no registrar dos veces el mismo compromiso
        if (compromisoRepo.existeDuplicado(persona.id(), empresa.getId(), request.concepto(), request.numeroCuota())) {
            throw new CompromisoDuplicado();
        }

        // 4. guardar pendiente con su folio y el nombre de la persona a la fecha
        String prefijo = "CP-" + hoy.getYear() + "-";
        Compromiso compromiso = compromisoRepo.save(Compromiso.builder()
                .folio(ReglasCompromiso.siguienteFolio(compromisoRepo.findFoliosConPrefijo(prefijo), hoy.getYear()))
                .empresa(empresa)
                .personaId(persona.id())
                .personaRut(persona.rut())
                .personaNombre(nombre(persona))
                .concepto(request.concepto().trim())
                .numeroCuota(request.numeroCuota())
                .totalCuotas(request.totalCuotas())
                .monto(request.monto())
                .fechaVencimiento(request.fechaVencimiento())
                .estado(EstadoCompromiso.PENDIENTE)
                .recargoAplicado(0)
                .fechaCambioEstado(ahora)
                .registradoPorId(ejecutivo.id())
                .registradoPorCorreo(correo(ejecutivo))
                .fechaRegistro(ahora)
                .build());

        return CompromisoMapper.toResponse(compromiso, hoy, persona);
    }

    // ---------- consultas RN-22 ----------
    @Override
    @Transactional(readOnly = true)
    public List<CompromisoResponse> listar(UUID ejecutivoId, YearMonth mes, EstadoCompromiso estado) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivoId);
        LocalDate hoy = LocalDate.now(clock);

        // 1. traer los del mes (o todos si no viene) y filtrar por el estado a la fecha
        List<Compromiso> compromisos = mes == null
                ? compromisoRepo.findDeEmpresa(empresa.getId())
                : compromisoRepo.findDeEmpresaEntre(empresa.getId(), mes.atDay(1), mes.atEndOfMonth());
        return compromisos.stream()
                .map(c -> CompromisoMapper.toResponse(c, hoy, null))
                .filter(c -> estado == null || c.estado() == estado)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CompromisoResponse detalle(UUID ejecutivoId, String folio) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivoId);
        Compromiso compromiso = deEmpresa(folio, empresa);

        // 1. el contacto se pide a usuarios; si no responde se muestra sin él
        PersonaInterna contacto;
        try {
            contacto = usuarios.porId(compromiso.getPersonaId()).orElse(null);
        } catch (RuntimeException ex) {
            contacto = null;
        }
        return CompromisoMapper.toResponse(compromiso, LocalDate.now(clock), contacto);
    }

    // ---------- anulación RN-21 ----------
    @Override
    @Transactional
    public CompromisoResponse anular(AuthenticatedUser ejecutivo, String folio, AnularCompromisoRequest request) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo.id());
        LocalDateTime ahora = LocalDateTime.now(clock);

        // 1. solo compromisos de su empresa, bloqueados mientras se anulan
        Compromiso compromiso = compromisoRepo.findByFolioParaActualizar(folio)
                .filter(c -> c.getEmpresa().getId().equals(empresa.getId()))
                .orElseThrow(() -> new CompromisoNoEncontrado(folio));
        EstadoCompromiso estado = CompromisoMapper.estado(compromiso, ahora.toLocalDate());

        // 2. validar estado y comentario
        if (!ReglasCompromiso.puedeAnularse(estado)) {
            throw new AnulacionNoPermitida(estado == EstadoCompromiso.PAGADO
                    ? "Un compromiso pagado no se puede anular"
                    : "Este compromiso ya está anulado");
        }
        String comentario = request.comentario() == null ? null : request.comentario().trim();
        if (request.motivo() == MotivoAnulacion.OTRO && (comentario == null || comentario.isEmpty())) {
            throw new AnulacionNoPermitida("Con el motivo Otro tienes que escribir un comentario");
        }

        // 3. guardar quién, por qué y cuándo, y dejarlo anulado
        Anulacion anulacion = anulacionRepo.save(Anulacion.builder()
                .compromiso(compromiso)
                .motivo(request.motivo())
                .comentario(comentario == null || comentario.isEmpty() ? null : comentario)
                .ejecutivoId(ejecutivo.id())
                .ejecutivoCorreo(correo(ejecutivo))
                .fecha(ahora)
                .build());
        compromiso.setEstado(EstadoCompromiso.ANULADO);
        compromiso.setFechaCambioEstado(ahora);
        compromiso.setAnulacion(anulacion);

        return CompromisoMapper.toResponse(compromiso, ahora.toLocalDate(), null);
    }

    // ---------- resumen del mes RN-24 ----------
    @Override
    @Transactional(readOnly = true)
    public ResumenCobranzaResponse resumen(UUID ejecutivoId, YearMonth mes) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivoId);
        LocalDate hoy = LocalDate.now(clock);
        List<CompromisoResponse> delMes = compromisoRepo
                .findDeEmpresaEntre(empresa.getId(), mes.atDay(1), mes.atEndOfMonth()).stream()
                .map(c -> CompromisoMapper.toResponse(c, hoy, null))
                .toList();

        // 1. contar por estado
        int pagados = contar(delMes, EstadoCompromiso.PAGADO);
        int anulados = contar(delMes, EstadoCompromiso.ANULADO);

        // 2. sumar lo pagado con sus recargos
        long recaudado = delMes.stream()
                .filter(c -> c.estado() == EstadoCompromiso.PAGADO)
                .mapToLong(CompromisoResponse::total)
                .sum();

        return new ResumenCobranzaResponse(
                empresa.getNombreFantasia(),
                mes,
                delMes.size(),
                pagados,
                contar(delMes, EstadoCompromiso.PENDIENTE),
                contar(delMes, EstadoCompromiso.VENCIDO),
                contar(delMes, EstadoCompromiso.EN_REVISION),
                anulados,
                ReglasCompromiso.tasaCobranza(pagados, delMes.size(), anulados),
                recaudado
        );
    }

    // --- helpers privados ---
    private Compromiso deEmpresa(String folio, EmpresaCliente empresa) {
        // un compromiso de otra empresa se trata como inexistente RN-22
        return compromisoRepo.findByFolio(folio)
                .filter(c -> c.getEmpresa().getId().equals(empresa.getId()))
                .orElseThrow(() -> new CompromisoNoEncontrado(folio));
    }

    private PersonaInterna nuevaPersona(String rut, RegistrarCompromisoRequest request) {
        // 1. con una persona nueva se piden sus datos básicos RN-20
        if (vacio(request.nombres()) || vacio(request.apellidos())
                || vacio(request.correo()) || vacio(request.telefono())) {
            throw new DatosCompromisoInvalidos(
                    "La persona no está registrada: ingresa nombres, apellidos, correo y teléfono");
        }
        return usuarios.crear(new SolicitudCrearPersonaInterna(rut, request.nombres().trim(), request.apellidos().trim(),
                EmailUtils.normalize(request.correo()), request.telefono().trim()));
    }

    private void validarCuotas(Integer numero, Integer total) {
        // 1. la cuota es opcional, pero si viene tiene que venir completa
        if (numero == null && total == null) return;
        if (numero == null || total == null) {
            throw new DatosCompromisoInvalidos("Ingresa la cuota y el total de cuotas");
        }
        // 2. la cuota no puede ser mayor que el total
        if (numero > total) {
            throw new DatosCompromisoInvalidos("La cuota no puede ser mayor que el total");
        }
    }

    private static String normalizarRut(String rut) {
        if (!RutUtils.isValid(rut)) throw new InvalidRut();
        return RutUtils.normalize(rut);
    }

    private static String nombre(PersonaInterna p) {
        return (p.nombres() + " " + p.apellidos()).trim();
    }

    private static String correo(AuthenticatedUser usuario) {
        return usuario.email() == null ? usuario.id().toString() : EmailUtils.normalize(usuario.email());
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static int contar(List<CompromisoResponse> lista, EstadoCompromiso estado) {
        return (int) lista.stream().filter(c -> c.estado() == estado).count();
    }
}
