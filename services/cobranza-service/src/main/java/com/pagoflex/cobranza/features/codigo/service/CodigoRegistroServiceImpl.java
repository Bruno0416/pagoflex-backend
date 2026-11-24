package com.pagoflex.cobranza.features.codigo.service;

import com.pagoflex.cobranza.features.codigo.entity.CodigoRegistro;
import com.pagoflex.cobranza.features.codigo.exceptions.CodigoInvalido;
import com.pagoflex.cobranza.features.codigo.repository.CodigoRegistroRepository;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.organizacion.CodigosEmpresaDemo;
import com.pagoflex.contracts.organizacion.RolAutorizado;
import com.pagoflex.contracts.organizacion.IdentificadoresEmpresa;
import com.pagoflex.contracts.organizacion.TipoOrganizacion;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import com.pagoflex.contracts.organizacion.RespuestaValidacionCodigoEmpresa;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CodigoRegistroServiceImpl implements CodigoRegistroService {
    private static final int INTENTOS_CODIGO = 20;
    private static final String LIMITE_GLOBAL = "global";

    private final CodigoRegistroRepository codigoRepo;
    private final EmpresaClienteRepository empresaRepo;
    private final PasswordEncoder encoder;
    private final LimitadorIntentosCodigo limitador;
    private final Clock clock;

    @Value("${pagoflex.security.internal-api-key}")
    private String claveReferencia;

    @Value("${pagoflex.demo.enabled:false}")
    private boolean demoEnabled;

    // hash de relleno para que un prefijo inexistente tarde lo mismo que uno existente
    private volatile String hashRelleno;

    // ---------- validación ----------
    @Override
    @Transactional(readOnly = true)
    public RespuestaValidacionCodigoEmpresa validar(String codigo) {
        // 1. limitar tambien los intentos que cambian de codigo en cada peticion
        if (!SolicitudValidacionCodigoEmpresa.esValido(codigo)) throw new CodigoInvalido();
        if (limitador.bloqueado(LIMITE_GLOBAL, 30)) throw new CodigoInvalido();
        boolean demo = demoEnabled && CodigosEmpresaDemo.contiene(codigo);
        String prefijo = GeneradorCodigo.referencia(codigo, claveReferencia, demo);
        if (limitador.bloqueado(prefijo)) throw new CodigoInvalido();

        // 2. una sola comparación BCrypt exista o no el registro
        Optional<CodigoRegistro> registro = codigoRepo.findByPrefijo(prefijo);
        boolean coincide = encoder.matches(codigo, registro.map(CodigoRegistro::getHash).orElseGet(this::relleno));

        // 3. activo, vigente y con organización y rol consistentes
        Optional<RespuestaValidacionCodigoEmpresa> respuesta = registro
                .filter(r -> coincide && r.isActivo() && !vencido(r))
                .flatMap(this::respuesta);
        if (respuesta.isEmpty()) {
            limitador.registrarFallo(prefijo);
            limitador.registrarFallo(LIMITE_GLOBAL);
            throw new CodigoInvalido();
        }
        limitador.limpiar(prefijo);
        return respuesta.get();
    }

    // ---------- generación interna ----------
    @Override
    @Transactional
    public String generarParaAgente(Instant expiraEn) {
        return generar(TipoOrganizacion.INTERNA, RolAutorizado.AGENTE, IdentificadoresEmpresa.INTERNA, expiraEn);
    }

    @Override
    @Transactional
    public String generarParaEjecutivo(UUID empresaId, Instant expiraEn) {
        // 1. el código apunta a una empresa cliente real
        if (empresaId == null || !empresaRepo.existsById(empresaId)) {
            throw new EmpresaNoEncontrada(String.valueOf(empresaId));
        }
        return generar(TipoOrganizacion.CLIENTE, RolAutorizado.EJECUTIVO, empresaId, expiraEn);
    }

    // ---------- aprovisionamiento demo ----------
    @Override
    @Transactional
    public void aprovisionarDemo(String empresa) {
        if (!demoEnabled) throw new IllegalStateException("Carga demo deshabilitada");
        String codigo = CodigosEmpresaDemo.deEmpresa(empresa);
        boolean interna = "INTERNA".equals(empresa);
        UUID empresaId = interna ? IdentificadoresEmpresa.INTERNA
                : empresaRepo.findByCodigo(empresa).orElseThrow(() -> new EmpresaNoEncontrada(empresa)).getId();
        TipoOrganizacion tipo = interna ? TipoOrganizacion.INTERNA : TipoOrganizacion.CLIENTE;
        RolAutorizado rol = interna ? RolAutorizado.AGENTE : RolAutorizado.EJECUTIVO;
        String prefijo = GeneradorCodigo.referencia(codigo, claveReferencia, true);

        // 1. no reutilizar un codigo normal ni sobrescribir un codigo revocado
        if (codigoRepo.findByPrefijo(GeneradorCodigo.referencia(codigo, claveReferencia, false))
                .filter(r -> encoder.matches(codigo, r.getHash())).isPresent()) {
            throw new IllegalStateException("Codigo demo ya asignado como codigo normal para " + empresa);
        }
        var existente = codigoRepo.findByPrefijo(prefijo);
        if (existente.isPresent()) {
            CodigoRegistro registro = existente.get();
            if (!empresaId.equals(registro.getEmpresaId()) || registro.getTipo() != tipo || registro.getRol() != rol
                    || !encoder.matches(codigo, registro.getHash())) {
                throw new IllegalStateException("Colision de codigo demo para " + empresa);
            }
            return;
        }

        // 2. guardar solo la referencia opaca y el hash bcrypt
        codigoRepo.save(CodigoRegistro.builder()
                .prefijo(prefijo)
                .hash(encoder.encode(codigo))
                .empresaId(empresaId)
                .tipo(tipo)
                .rol(rol)
                .activo(true)
                .creadoEn(Instant.now(clock))
                .expiraEn(null)
                .build());
    }

    @Override
    @Transactional
    public void revocar(String codigo) {
        if (!SolicitudValidacionCodigoEmpresa.esValido(codigo)) throw new CodigoInvalido();
        for (boolean demo : new boolean[]{false, true}) {
            codigoRepo.findByPrefijo(GeneradorCodigo.referencia(codigo, claveReferencia, demo))
                    .filter(r -> encoder.matches(codigo, r.getHash()))
                    .ifPresent(r -> r.setActivo(false));
        }
    }

    // --- helpers privados ---
    private String generar(TipoOrganizacion tipo, RolAutorizado rol, UUID empresaId, Instant expiraEn) {
        // 1. codigo de seis caracteres con referencia de busqueda libre
        String codigo = codigoLibre();
        String prefijo = GeneradorCodigo.referencia(codigo, claveReferencia, false);

        // 2. se guarda solo el hash del código completo
        codigoRepo.save(CodigoRegistro.builder()
                .prefijo(prefijo)
                .hash(encoder.encode(codigo))
                .empresaId(empresaId)
                .tipo(tipo)
                .rol(rol)
                .activo(true)
                .expiraEn(expiraEn)
                .creadoEn(Instant.now(clock))
                .build());
        return codigo;
    }

    private String codigoLibre() {
        for (int i = 0; i < INTENTOS_CODIGO; i++) {
            String codigo = GeneradorCodigo.codigoAleatorio();
            if (!CodigosEmpresaDemo.contiene(codigo)
                    && !codigoRepo.existsByPrefijo(GeneradorCodigo.referencia(codigo, claveReferencia, false))
                    && !codigoRepo.existsByPrefijo(GeneradorCodigo.referencia(codigo, claveReferencia, true))) return codigo;
        }
        throw new IllegalStateException("No hay referencias de codigo libres");
    }

    private Optional<RespuestaValidacionCodigoEmpresa> respuesta(CodigoRegistro r) {
        // 1. solo se aceptan INTERNA + AGENTE y CLIENTE + EJECUTIVO
        if (r.getTipo() == TipoOrganizacion.INTERNA && r.getRol() == RolAutorizado.AGENTE
                && IdentificadoresEmpresa.INTERNA.equals(r.getEmpresaId())) {
            return Optional.of(new RespuestaValidacionCodigoEmpresa(IdentificadoresEmpresa.INTERNA,
                    IdentificadoresEmpresa.NOMBRE_INTERNA, r.getTipo(), r.getRol()));
        }
        if (r.getTipo() == TipoOrganizacion.CLIENTE && r.getRol() == RolAutorizado.EJECUTIVO) {
            // 2. la empresa cliente tiene que existir en cobranza
            return empresaRepo.findById(r.getEmpresaId())
                    .map(EmpresaCliente::getNombreFantasia)
                    .map(nombre -> new RespuestaValidacionCodigoEmpresa(
                            r.getEmpresaId(), nombre, r.getTipo(), r.getRol()));
        }
        return Optional.empty();
    }

    private boolean vencido(CodigoRegistro r) {
        return r.getExpiraEn() != null && !r.getExpiraEn().isAfter(Instant.now(clock));
    }

    private String relleno() {
        if (hashRelleno == null) {
            hashRelleno = encoder.encode(GeneradorCodigo.codigoAleatorio());
        }
        return hashRelleno;
    }
}
