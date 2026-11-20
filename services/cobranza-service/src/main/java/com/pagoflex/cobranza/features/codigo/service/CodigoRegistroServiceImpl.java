package com.pagoflex.cobranza.features.codigo.service;

import com.pagoflex.cobranza.features.codigo.entity.CodigoRegistro;
import com.pagoflex.cobranza.features.codigo.exceptions.CodigoInvalido;
import com.pagoflex.cobranza.features.codigo.repository.CodigoRegistroRepository;
import com.pagoflex.cobranza.features.empresa.entity.EmpresaCliente;
import com.pagoflex.cobranza.features.empresa.exceptions.EmpresaNoEncontrada;
import com.pagoflex.cobranza.features.empresa.repository.EmpresaClienteRepository;
import com.pagoflex.contracts.organization.AuthorizedRole;
import com.pagoflex.contracts.organization.IdentificadoresEmpresa;
import com.pagoflex.contracts.organization.OrganizationType;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeRequest;
import com.pagoflex.contracts.organization.ValidateOrganizationCodeResponse;
import lombok.RequiredArgsConstructor;
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
    private static final int INTENTOS_PREFIJO = 20;

    private final CodigoRegistroRepository codigoRepo;
    private final EmpresaClienteRepository empresaRepo;
    private final PasswordEncoder encoder;
    private final LimitadorIntentosCodigo limitador;
    private final Clock clock;

    // hash de relleno para que un prefijo inexistente tarde lo mismo que uno existente
    private volatile String hashRelleno;

    // ---------- validación ----------
    @Override
    @Transactional(readOnly = true)
    public ValidateOrganizationCodeResponse validar(String codigo) {
        // 1. formato y bloqueo por demasiados fallos del mismo prefijo
        if (!ValidateOrganizationCodeRequest.esValido(codigo)) throw new CodigoInvalido();
        String prefijo = GeneradorCodigo.prefijoDe(codigo);
        if (limitador.bloqueado(prefijo)) throw new CodigoInvalido();

        // 2. una sola comparación BCrypt exista o no el registro
        Optional<CodigoRegistro> registro = codigoRepo.findByPrefijo(prefijo);
        boolean coincide = encoder.matches(codigo, registro.map(CodigoRegistro::getHash).orElseGet(this::relleno));

        // 3. activo, vigente y con organización y rol consistentes
        Optional<ValidateOrganizationCodeResponse> respuesta = registro
                .filter(r -> coincide && r.isActivo() && !vencido(r))
                .flatMap(this::respuesta);
        if (respuesta.isEmpty()) {
            limitador.registrarFallo(prefijo);
            throw new CodigoInvalido();
        }
        limitador.limpiar(prefijo);
        return respuesta.get();
    }

    // ---------- generación interna ----------
    @Override
    @Transactional
    public String generarParaAgente(Instant expiraEn) {
        return generar(OrganizationType.INTERNA, AuthorizedRole.AGENTE, IdentificadoresEmpresa.INTERNA, expiraEn);
    }

    @Override
    @Transactional
    public String generarParaEjecutivo(UUID empresaId, Instant expiraEn) {
        // 1. el código apunta a una empresa cliente real
        if (empresaId == null || !empresaRepo.existsById(empresaId)) {
            throw new EmpresaNoEncontrada(String.valueOf(empresaId));
        }
        return generar(OrganizationType.CLIENTE, AuthorizedRole.EJECUTIVO, empresaId, expiraEn);
    }

    @Override
    @Transactional
    public void revocar(String prefijo) {
        codigoRepo.findByPrefijo(prefijo).ifPresent(r -> r.setActivo(false));
    }

    // --- helpers privados ---
    private String generar(OrganizationType tipo, AuthorizedRole rol, UUID empresaId, Instant expiraEn) {
        // 1. prefijo libre y secreto de 128 bits
        String prefijo = prefijoLibre();
        String codigo = GeneradorCodigo.armar(prefijo, GeneradorCodigo.secretoAleatorio());

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

    private String prefijoLibre() {
        for (int i = 0; i < INTENTOS_PREFIJO; i++) {
            String prefijo = GeneradorCodigo.prefijoAleatorio();
            if (!codigoRepo.existsByPrefijo(prefijo)) return prefijo;
        }
        throw new IllegalStateException("No hay prefijos de código libres");
    }

    private Optional<ValidateOrganizationCodeResponse> respuesta(CodigoRegistro r) {
        // 1. solo se aceptan INTERNA + AGENTE y CLIENTE + EJECUTIVO
        if (r.getTipo() == OrganizationType.INTERNA && r.getRol() == AuthorizedRole.AGENTE
                && IdentificadoresEmpresa.INTERNA.equals(r.getEmpresaId())) {
            return Optional.of(new ValidateOrganizationCodeResponse(IdentificadoresEmpresa.INTERNA,
                    IdentificadoresEmpresa.NOMBRE_INTERNA, r.getTipo(), r.getRol()));
        }
        if (r.getTipo() == OrganizationType.CLIENTE && r.getRol() == AuthorizedRole.EJECUTIVO) {
            // 2. la empresa cliente tiene que existir en cobranza
            return empresaRepo.findById(r.getEmpresaId())
                    .map(EmpresaCliente::getNombreFantasia)
                    .map(nombre -> new ValidateOrganizationCodeResponse(
                            r.getEmpresaId(), nombre, r.getTipo(), r.getRol()));
        }
        return Optional.empty();
    }

    private boolean vencido(CodigoRegistro r) {
        return r.getExpiraEn() != null && !r.getExpiraEn().isAfter(Instant.now(clock));
    }

    private String relleno() {
        if (hashRelleno == null) {
            hashRelleno = encoder.encode(GeneradorCodigo.armar("AA00AA", GeneradorCodigo.secretoAleatorio()));
        }
        return hashRelleno;
    }
}
