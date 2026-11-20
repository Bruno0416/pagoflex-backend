package com.pagoflex.usuarios.features.internal.service;

import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.internal.dto.request.CreatePersonRequest;
import com.pagoflex.usuarios.features.internal.exceptions.PersonNotFound;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalPersonServiceTest {
    @Mock PersonaRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks InternalPersonService service;

    // ---------- crear persona sin cuenta ----------
    @Test
    void createsPersonWithoutUserAndNormalizesRutAndEmail() {
        when(repo.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(new CreatePersonRequest("99.999.999-9", "Usuario", "Prueba",
                " USUARIO@EXAMPLE.COM ", "+56912345678"));
        assertThat(result.rut()).isEqualTo("99999999-9");
        assertThat(result.correoContacto()).isEqualTo("usuario@example.com");
        verify(repo).saveAndFlush(any(Persona.class));
    }

    @Test
    void rejectsExistingRut() {
        when(repo.existsByRut("99999999-9")).thenReturn(true);
        assertThatThrownBy(() -> service.create(new CreatePersonRequest("99999999-9", "Usuario", "Prueba",
                null, "+56912345678"))).isInstanceOf(RutAlreadyInUse.class);
        verify(repo, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInvalidRut() {
        assertThatThrownBy(() -> service.create(new CreatePersonRequest("99999999-8", "Usuario", "Prueba",
                null, "+56912345678"))).isInstanceOf(InvalidRut.class);
        verifyNoInteractions(repo);
    }

    // ---------- consultas internas ----------
    @Test
    void findsPersonByRut() {
        Persona persona = Persona.builder().id(UUID.randomUUID()).rut("99999999-9").nombres("Prueba").build();
        when(repo.findByRut("99999999-9")).thenReturn(Optional.of(persona));
        assertThat(service.byRut("99.999.999-9").idPersona()).isEqualTo(persona.getId());
    }

    @Test
    void findsPersonById() {
        UUID id = UUID.randomUUID();
        when(repo.findById(id)).thenReturn(Optional.of(Persona.builder().id(id).build()));
        assertThat(service.byId(id).idPersona()).isEqualTo(id);
    }

    @Test
    void returnsNotFoundForUnknownPerson() {
        assertThatThrownBy(() -> service.byRut("99999999-9")).isInstanceOf(PersonNotFound.class);
        assertThatThrownBy(() -> service.byId(UUID.randomUUID())).isInstanceOf(PersonNotFound.class);
    }

    @Test
    void createsPersonWithoutContactEmail() {
        when(repo.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        var result = service.create(new CreatePersonRequest("99999999-9", "Usuario", "Prueba", null, "+56912345678"));
        assertThat(result.correoContacto()).isNull();
        assertThat(result.idUsuario()).isNull();
    }

    // ---------- personas con cuenta ----------
    @Test
    void findsPersonByUserIdWithAccountData() {
        Persona persona = Persona.builder().id(UUID.randomUUID()).rut("99999999-9").build();
        Users user = Users.builder().id(UUID.randomUUID()).email("a@b.cl").role(Role.USUARIO_FINAL).persona(persona).build();
        when(userRepo.findById(user.getId())).thenReturn(Optional.of(user));
        var result = service.byUserId(user.getId());
        assertThat(result.idPersona()).isEqualTo(persona.getId());
        assertThat(result.idUsuario()).isEqualTo(user.getId());
        assertThat(result.correoCuenta()).isEqualTo("a@b.cl");
    }

    @Test
    void findsPersonByAccountEmail() {
        Persona persona = Persona.builder().id(UUID.randomUUID()).rut("99999999-9").build();
        Users user = Users.builder().id(UUID.randomUUID()).email("a@b.cl").role(Role.USUARIO_FINAL).persona(persona).build();
        when(userRepo.findByEmail("a@b.cl")).thenReturn(Optional.of(user));
        assertThat(service.byEmail(" A@B.cl ").idUsuario()).isEqualTo(user.getId());
    }

    @Test
    void personByIdIncludesAccountWhenItExists() {
        Persona persona = Persona.builder().id(UUID.randomUUID()).rut("99999999-9").build();
        Users user = Users.builder().id(UUID.randomUUID()).email("a@b.cl").role(Role.USUARIO_FINAL).persona(persona).build();
        when(repo.findById(persona.getId())).thenReturn(Optional.of(persona));
        when(userRepo.findByPersona_Id(persona.getId())).thenReturn(Optional.of(user));
        assertThat(service.byId(persona.getId()).idUsuario()).isEqualTo(user.getId());
    }

    @Test
    void unknownAccountIsNotFound() {
        assertThatThrownBy(() -> service.byUserId(UUID.randomUUID())).isInstanceOf(PersonNotFound.class);
        assertThatThrownBy(() -> service.byEmail("x@y.cl")).isInstanceOf(PersonNotFound.class);
    }
}
