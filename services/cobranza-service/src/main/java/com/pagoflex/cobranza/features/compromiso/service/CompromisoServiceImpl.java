package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.*;
import com.pagoflex.cobranza.features.compromiso.exceptions.*;
import com.pagoflex.cobranza.features.compromiso.repository.AnulacionRepository;
import com.pagoflex.cobranza.features.compromiso.repository.CompromisoRepository;
import com.pagoflex.cobranza.features.compromiso.repository.PersonaRepository;
import com.pagoflex.cobranza.features.compromiso.rules.ReglasCompromiso;
import com.pagoflex.cobranza.features.empresa.dto.response.EmpresaResponse;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.service.EjecutivoService;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.core.utils.RutUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CompromisoServiceImpl implements CompromisoService {
    private final CompromisoRepository compromisoRepo;
    private final PersonaRepository personaRepo;
    private final AnulacionRepository anulacionRepo;
    private final EjecutivoService ejecutivoService;
    private final Clock clock;

    // ---------- registro RN-20 ----------
    @Override
    @Transactional
    public CompromisoResponse registrar(String ejecutivo, RegistrarCompromisoRequest request) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDate hoy = ahora.toLocalDate();

        // 1. normalizar el rut
        String rut;
        try {
            rut = RutUtils.normalize(request.rut());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRut();
        }

        // 2. validar fecha y cuotas
        if (request.fechaVencimiento().isBefore(hoy)) {
            throw new DatosCompromisoInvalidos("El vencimiento no puede ser anterior a hoy");
        }
        validarCuotas(request.numeroCuota(), request.totalCuotas());

        // 3. usar la persona registrada o pedir sus datos básicos
        Persona persona = personaRepo.findByRut(rut).orElseGet(() -> nuevaPersona(rut, request));

        // 4. no registrar dos veces el mismo compromiso
        if (persona.getId() != null && compromisoRepo.existeDuplicado(
                persona.getId(), empresa.getId(), request.concepto(), request.numeroCuota())) {
            throw new CompromisoDuplicado();
        }

        // 5. guardar la persona nueva y el compromiso pendiente con su folio
        if (persona.getId() == null) persona = personaRepo.save(persona);
        String prefijo = "CP-" + hoy.getYear() + "-";
        Compromiso compromiso = compromisoRepo.save(Compromiso.builder()
                .folio(ReglasCompromiso.siguienteFolio(compromisoRepo.findFoliosConPrefijo(prefijo), hoy.getYear()))
                .empresa(empresa)
                .persona(persona)
                .concepto(request.concepto().trim())
                .numeroCuota(request.numeroCuota())
                .totalCuotas(request.totalCuotas())
                .monto(request.monto())
                .fechaVencimiento(request.fechaVencimiento())
                .estado(EstadoCompromiso.PENDIENTE)
                .recargoAplicado(0)
                .fechaCambioEstado(ahora)
                .registradoPor(EmailUtils.normalize(ejecutivo))
                .fechaRegistro(ahora)
                .build());

        return CompromisoMapper.toResponse(compromiso, hoy);
    }

    // ---------- consultas RN-22 ----------
    @Override
    public List<CompromisoResponse> listar(String ejecutivo, YearMonth mes, EstadoCompromiso estado) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        LocalDate hoy = LocalDate.now(clock);

        // 1. traer los del mes (o todos si no viene) y filtrar por el estado a la fecha
        List<Compromiso> compromisos = mes == null
                ? compromisoRepo.findDeEmpresa(empresa.getId())
                : compromisoRepo.findDeEmpresaEntre(empresa.getId(), mes.atDay(1), mes.atEndOfMonth());
        return compromisos.stream()
                .map(c -> CompromisoMapper.toResponse(c, hoy))
                .filter(c -> estado == null || c.estado() == estado)
                .toList();
    }

    @Override
    public CompromisoResponse detalle(String ejecutivo, String folio) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        return CompromisoMapper.toResponse(deEmpresa(folio, empresa), LocalDate.now(clock));
    }

    // ---------- anulación RN-21 ----------
    @Override
    @Transactional
    public CompromisoResponse anular(String ejecutivo, String folio, AnularCompromisoRequest request) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        LocalDateTime ahora = LocalDateTime.now(clock);

        // 1. solo compromisos de su empresa
        Compromiso compromiso = deEmpresa(folio, empresa);
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
                .ejecutivo(EmailUtils.normalize(ejecutivo))
                .fecha(ahora)
                .build());
        compromiso.setEstado(EstadoCompromiso.ANULADO);
        compromiso.setFechaCambioEstado(ahora);
        compromiso.setAnulacion(anulacion);

        return CompromisoMapper.toResponse(compromiso, ahora.toLocalDate());
    }

    // ---------- resumen del mes RN-24 ----------
    @Override
    public ResumenCobranzaResponse resumen(String ejecutivo, YearMonth mes) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        LocalDate hoy = LocalDate.now(clock);
        List<CompromisoResponse> delMes = compromisoRepo
                .findDeEmpresaEntre(empresa.getId(), mes.atDay(1), mes.atEndOfMonth()).stream()
                .map(c -> CompromisoMapper.toResponse(c, hoy))
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

    // ---------- apoyo a las pantallas del ejecutivo ----------
    @Override
    public EmpresaResponse empresa(String ejecutivo) {
        EmpresaCliente empresa = ejecutivoService.empresaDe(ejecutivo);
        return new EmpresaResponse(empresa.getCodigo(), empresa.getNombreFantasia(), empresa.getRecargoAtraso());
    }

    @Override
    public PersonaEncontradaResponse buscarPersona(String ejecutivo, String rut) {
        // 1. solo un ejecutivo con empresa puede buscar
        ejecutivoService.empresaDe(ejecutivo);

        // 2. buscar por rut normalizado
        String normalizado;
        try {
            normalizado = RutUtils.normalize(rut);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRut();
        }
        return personaRepo.findByRut(normalizado)
                .map(p -> new PersonaEncontradaResponse(p.nombreCompleto(), p.getCorreo()))
                .orElseThrow(PersonaNoEncontrada::new);
    }

    // ---------- usuario final ----------
    @Override
    public List<CompromisoResponse> deUsuario(String correo) {
        if (correo == null) return List.of();
        LocalDate hoy = LocalDate.now(clock);
        return compromisoRepo.findDePersona(EmailUtils.normalize(correo)).stream()
                .map(c -> CompromisoMapper.toResponse(c, hoy))
                .toList();
    }

    // --- helpers privados ---
    private Compromiso deEmpresa(String folio, EmpresaCliente empresa) {
        // un compromiso de otra empresa se trata como inexistente RN-22
        return compromisoRepo.findByFolio(folio)
                .filter(c -> c.getEmpresa().getId().equals(empresa.getId()))
                .orElseThrow(() -> new CompromisoNoEncontrado(folio));
    }

    private Persona nuevaPersona(String rut, RegistrarCompromisoRequest request) {
        // 1. con una persona nueva se piden sus datos básicos RN-20
        if (vacio(request.nombres()) || vacio(request.apellidos())
                || vacio(request.correo()) || vacio(request.telefono())) {
            throw new DatosCompromisoInvalidos(
                    "La persona no está registrada: ingresa nombres, apellidos, correo y teléfono");
        }
        return Persona.builder()
                .rut(rut)
                .nombres(request.nombres().trim())
                .apellidos(request.apellidos().trim())
                .correo(EmailUtils.normalize(request.correo()))
                .telefono(request.telefono().trim())
                .build();
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

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static int contar(List<CompromisoResponse> lista, EstadoCompromiso estado) {
        return (int) lista.stream().filter(c -> c.estado() == estado).count();
    }
}
