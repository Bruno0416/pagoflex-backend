package com.pagoflex.usuarios.features.auth.service;

import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class RegistrationPersistenceTest {
    @Autowired AuthService service;
    @Autowired UserRepository userRepo;
    @Autowired PersonaRepository personaRepo;

    // ---------- persistencia del registro ----------
    @Test
    void registerPersistsLinkedUserAndPersona() {
        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null));

        var user = userRepo.findByEmail("usuario@example.com").orElseThrow();
        assertThat(user.getPersona()).isNotNull();
        assertThat(user.getPersona().getRut()).isEqualTo("99999999-9");
        assertThat(user.getPersona().getId()).isNotNull();
        assertThat(user.getPassword()).isNotEqualTo("Clave123!");
    }

    @Test
    void rejectsDuplicateRutWithoutCreatingAnotherUser() {
        service.register(new RegisterRequest("primero@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null));
        long usersBefore = userRepo.count();
        long peopleBefore = personaRepo.count();

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "segundo@example.com", "Clave123!", "99999999-9", "Segundo", "Prueba", null)))
                .isInstanceOf(RutAlreadyInUse.class);
        assertThat(userRepo.count()).isEqualTo(usersBefore);
        assertThat(personaRepo.count()).isEqualTo(peopleBefore);
    }

    @Test
    void rejectsDuplicateEmailWithoutCreatingAnotherPersona() {
        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null));
        long peopleBefore = personaRepo.count();

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "usuario@example.com", "Clave123!", "99999999-9", "Segundo", "Prueba", null)))
                .isInstanceOf(EmailAlreadyInUse.class);
        assertThat(personaRepo.count()).isEqualTo(peopleBefore);
    }
}
