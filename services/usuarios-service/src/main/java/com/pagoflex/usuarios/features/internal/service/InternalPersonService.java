package com.pagoflex.usuarios.features.internal.service;

import com.pagoflex.usuarios.features.internal.dto.request.CreatePersonRequest;
import com.pagoflex.usuarios.features.internal.dto.response.PersonResponse;
import com.pagoflex.usuarios.features.internal.exceptions.PersonNotFound;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
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
    private final UserRepository userRepository;

    // ---------- alta de persona sin cuenta ----------
    @Transactional
    public PersonResponse create(CreatePersonRequest request) {
        // 1. normalizar y verificar el rut
        String rut = normalize(request.rut());
        if (repository.existsByRut(rut)) throw new RutAlreadyInUse();
        Persona persona = Persona.builder().rut(rut)
                .nombres(request.nombres().trim()).apellidos(request.apellidos().trim())
                .correoContacto(request.correoContacto() == null || request.correoContacto().isBlank()
                        ? null : EmailUtils.normalize(request.correoContacto()))
                .telefono(request.telefono().trim()).build();
        // 2. persistir la persona sin crear credenciales
        try { return toResponse(repository.saveAndFlush(persona), null); }
        catch (DataIntegrityViolationException ex) { throw new RutAlreadyInUse(); }
    }

    // ---------- busqueda interna ----------
    @Transactional(readOnly = true)
    public PersonResponse byRut(String rut) {
        return repository.findByRut(normalize(rut)).map(this::withAccount).orElseThrow(PersonNotFound::new);
    }

    @Transactional(readOnly = true)
    public PersonResponse byId(UUID id) {
        return repository.findById(id).map(this::withAccount).orElseThrow(PersonNotFound::new);
    }

    @Transactional(readOnly = true)
    public PersonResponse byUserId(UUID userId) {
        // 1. la persona enlazada a la cuenta
        Users user = userRepository.findById(userId).orElseThrow(PersonNotFound::new);
        if (user.getPersona() == null) throw new PersonNotFound();
        return toResponse(user.getPersona(), user);
    }

    @Transactional(readOnly = true)
    public PersonResponse byEmail(String email) {
        // 1. el correo de acceso identifica la cuenta
        Users user = userRepository.findByEmail(EmailUtils.normalize(email)).orElseThrow(PersonNotFound::new);
        if (user.getPersona() == null) throw new PersonNotFound();
        return toResponse(user.getPersona(), user);
    }

    // --- helpers ---
    private static String normalize(String rut) {
        try { return RutUtils.normalize(rut); }
        catch (IllegalArgumentException ex) { throw new InvalidRut(); }
    }

    private PersonResponse withAccount(Persona person) {
        return toResponse(person, userRepository.findByPersona_Id(person.getId()).orElse(null));
    }

    private static PersonResponse toResponse(Persona person, Users user) {
        return new PersonResponse(person.getId(), person.getRut(), person.getNombres(),
                person.getApellidos(), person.getCorreoContacto(), person.getTelefono(),
                user != null ? user.getId() : null, user != null ? user.getEmail() : null,
                person.getEmpresaId());
    }
}
