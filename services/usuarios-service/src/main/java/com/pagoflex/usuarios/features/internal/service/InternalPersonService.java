package com.pagoflex.usuarios.features.internal.service;

import com.pagoflex.usuarios.features.internal.dto.request.CreatePersonRequest;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.internal.dto.response.AccountPersonResponse;
import com.pagoflex.usuarios.features.internal.dto.response.PersonResponse;
import com.pagoflex.usuarios.features.internal.exceptions.PersonNotFound;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import com.pagoflex.core.utils.RutUtils;
import com.pagoflex.core.utils.EmailUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalPersonService {
    private final PersonaRepository repository;
    private final UserRepository users;

    // ---------- alta de persona sin cuenta ----------
    @Transactional
    public PersonResponse create(CreatePersonRequest request) {
        // 1. normalizar y verificar el rut
        String rut = normalize(request.rut());
        if (repository.existsByRut(rut)) throw new RutAlreadyInUse();
        Persona persona = Persona.builder().rut(rut)
                .nombres(request.nombres().trim()).apellidos(request.apellidos().trim())
                .correoContacto(EmailUtils.normalize(request.correoContacto()))
                .telefono(request.telefono().trim()).build();
        // 2. persistir la persona sin crear credenciales
        try { return toResponse(repository.saveAndFlush(persona)); }
        catch (DataIntegrityViolationException ex) { throw new RutAlreadyInUse(); }
    }

    // ---------- busqueda interna ----------
    @Transactional(readOnly = true)
    public PersonResponse byRut(String rut) {
        return repository.findByRut(normalize(rut)).map(InternalPersonService::toResponse)
                .orElseThrow(PersonNotFound::new);
    }

    @Transactional(readOnly = true)
    public PersonResponse byId(UUID id) {
        return repository.findById(id).map(InternalPersonService::toResponse)
                .orElseThrow(PersonNotFound::new);
    }

    // ---------- persona de una cuenta ----------
    @Transactional(readOnly = true)
    public AccountPersonResponse byUser(UUID userId) {
        return users.findById(userId).map(InternalPersonService::toAccount).orElseThrow(PersonNotFound::new);
    }

    @Transactional(readOnly = true)
    public AccountPersonResponse byEmail(String email) {
        return users.findByEmail(EmailUtils.normalize(email)).map(InternalPersonService::toAccount)
                .orElseThrow(PersonNotFound::new);
    }

    // --- helpers ---
    private static String normalize(String rut) {
        try { return RutUtils.normalize(rut); }
        catch (IllegalArgumentException ex) { throw new InvalidRut(); }
    }

    private static AccountPersonResponse toAccount(Users user) {
        // 1. una cuenta sin persona no sirve para cobranza ni soporte
        Persona person = user.getPersona();
        if (person == null) throw new PersonNotFound();
        return new AccountPersonResponse(user.getId(), person.getId(), person.getRut(), person.getNombres(),
                person.getApellidos(), user.getEmail(), person.getTelefono(), person.getEmpresaId(),
                user.getRole().name());
    }

    private static PersonResponse toResponse(Persona person) {
        return new PersonResponse(person.getId(), person.getRut(), person.getNombres(),
                person.getApellidos(), person.getCorreoContacto(), person.getTelefono());
    }
}
