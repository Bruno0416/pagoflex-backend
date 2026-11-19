package com.pagoflex.usuarios.features.internal.service;

import com.pagoflex.contracts.usuarios.CrearPersonaInterna;
import com.pagoflex.contracts.usuarios.PersonaInterna;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.core.utils.RutUtils;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.internal.exceptions.PersonaNoEncontrada;
import com.pagoflex.usuarios.features.internal.exceptions.PersonaYaExiste;
import com.pagoflex.usuarios.features.internal.exceptions.RutInvalido;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PersonaInternaServiceImpl implements PersonaInternaService {
    private final PersonaRepository personaRepo;
    private final UserRepository userRepo;

    // ---------- consultas ----------
    @Override
    @Transactional(readOnly = true)
    public PersonaInterna porId(UUID id) {
        return personaRepo.findById(id).map(this::aInterna).orElseThrow(PersonaNoEncontrada::new);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaInterna porUsuario(UUID usuarioId) {
        // 1. la persona enlazada a la cuenta del token
        Users usuario = userRepo.findById(usuarioId).orElseThrow(PersonaNoEncontrada::new);
        if (usuario.getPersona() == null) throw new PersonaNoEncontrada();
        return aInterna(usuario.getPersona(), usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaInterna porRut(String rut) {
        return personaRepo.findByRut(normalizarRut(rut)).map(this::aInterna).orElseThrow(PersonaNoEncontrada::new);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonaInterna porCorreo(String correo) {
        Users usuario = userRepo.findByEmail(EmailUtils.normalize(correo)).orElseThrow(PersonaNoEncontrada::new);
        if (usuario.getPersona() == null) throw new PersonaNoEncontrada();
        return aInterna(usuario.getPersona(), usuario);
    }

    // ---------- alta RN-20 ----------
    @Override
    @Transactional
    public PersonaInterna crear(CrearPersonaInterna request) {
        // 1. un rut solo puede existir una vez
        String rut = normalizarRut(request.rut());
        if (personaRepo.existsByRut(rut)) throw new PersonaYaExiste();

        // 2. persona sin cuenta con su correo de contacto
        try {
            Persona persona = personaRepo.saveAndFlush(Persona.builder()
                    .rut(rut)
                    .nombres(request.nombres().trim())
                    .apellidos(request.apellidos().trim())
                    .telefono(request.telefono().trim())
                    .correoContacto(request.correo() == null || request.correo().isBlank()
                            ? null : EmailUtils.normalize(request.correo()))
                    .build());
            return aInterna(persona, null);
        } catch (DataIntegrityViolationException ex) {
            // 3. otro registro ganó la carrera por el mismo rut
            throw new PersonaYaExiste();
        }
    }

    // --- helpers privados ---
    private String normalizarRut(String rut) {
        if (!RutUtils.isValid(rut)) throw new RutInvalido();
        return RutUtils.normalize(rut);
    }

    private PersonaInterna aInterna(Persona persona) {
        return aInterna(persona, userRepo.findByPersona_Id(persona.getId()).orElse(null));
    }

    private PersonaInterna aInterna(Persona persona, Users usuario) {
        // 1. con cuenta el correo es el de acceso; sin cuenta, el de contacto
        return new PersonaInterna(
                persona.getId(),
                persona.getRut(),
                persona.getNombres(),
                persona.getApellidos(),
                usuario != null ? usuario.getEmail() : persona.getCorreoContacto(),
                persona.getTelefono(),
                usuario != null ? usuario.getId() : null);
    }
}
