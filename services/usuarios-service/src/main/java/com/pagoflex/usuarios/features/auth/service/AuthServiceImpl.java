package com.pagoflex.usuarios.features.auth.service;

import com.pagoflex.contracts.organizacion.RolAutorizado;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import com.pagoflex.contracts.organizacion.TipoOrganizacion;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.core.utils.RutUtils;
import com.pagoflex.usuarios.core.client.OrganizationClient;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.*;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepo;
    private final PersonaRepository personaRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final OrganizationClient organizationClient;
    private final AuthSessionService sessions;

    // ---------- registro ----------
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. normalizar correo y rut
        String email = EmailUtils.normalize(request.correo());
        String rut;
        try { rut = RutUtils.normalize(request.rut()); }
        catch (IllegalArgumentException ex) { throw new InvalidRut(); }

        // 2. rechazar identidades duplicadas
        if (userRepo.existsByEmail(email)) throw new EmailAlreadyInUse(email);
        var existente = personaRepo.findByRutForUpdate(rut);
        // Las personas precreadas por Cobranza solo pueden vincular
        // una cuenta de usuario final, nunca obtener un rol interno.
        if (existente.isPresent() && request.codigoEmpresa() != null) {
            throw new RutAlreadyInUse();
        }

        // 3. decidir el rol exclusivamente desde el codigo validado en cobranza
        Role role = Role.USUARIO_FINAL;
        UUID empresaId = null;
        if (request.codigoEmpresa() != null) {
            if (!SolicitudValidacionCodigoEmpresa.esValido(request.codigoEmpresa())) {
                throw new InvalidOrganizationCode();
            }
            var valid = organizationClient.validate(request.codigoEmpresa());
            if (valid.tipoEmpresa() == TipoOrganizacion.INTERNA && valid.rolAutorizado() == RolAutorizado.AGENTE) {
                role = Role.AGENTE;
            } else if (valid.tipoEmpresa() == TipoOrganizacion.CLIENTE
                    && valid.rolAutorizado() == RolAutorizado.EJECUTIVO) {
                role = Role.EJECUTIVO;
            } else {
                throw new InvalidOrganizationCode();
            }
            empresaId = valid.idEmpresa();
        }

        // 4. guardar persona y cuenta vinculada en una transaccion local
        Persona persona;
        if (existente.isPresent()) {
            persona = existente.get();
            String contacto = EmailUtils.normalize(persona.getCorreoContacto());
            if (persona.getEmpresaId() != null
                    || contacto == null
                    || !email.equals(contacto)
                    || userRepo.existsByPersona_Id(persona.getId())) {
                throw new RutAlreadyInUse();
            }
            // No modificar UUID, nombre, contacto ni relaciones existentes.
        } else {
            try {
                persona = personaRepo.saveAndFlush(Persona.builder()
                        .rut(rut)
                        .nombres(request.nombres().trim())
                        .apellidos(request.apellidos().trim())
                        .correoContacto(email)
                        .empresaId(empresaId)
                        .build());
            } catch (DataIntegrityViolationException ex) {
                throw new RutAlreadyInUse();
            }
        }
        try {
            userRepo.saveAndFlush(Users.builder().email(email)
                    .password(passwordEncoder.encode(request.contrasena()))
                    .role(role).persona(persona).build());
        } catch (DataIntegrityViolationException ex) {
            throw new EmailAlreadyInUse(email);
        }
    }

    // ---------- inicio y cierre de sesion ----------
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = EmailUtils.normalize(request.correo());
        Users user = userRepo.findByEmail(email).orElseThrow(InvalidCredentials::new);
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.contrasena()));
        } catch (org.springframework.security.core.AuthenticationException ex) {
            throw new InvalidCredentials();
        }
        return sessions.issue(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String token) { return sessions.refresh(token); }

    @Override
    public void logout() { sessions.revokeCurrent(); }
}
