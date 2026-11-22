package com.pagoflex.usuarios.features.auth.data;

import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CargaDemoTest {
    private final UserRepository userRepo = mock(UserRepository.class);
    private final PersonaRepository personaRepo = mock(PersonaRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final CargaDemo carga = new CargaDemo(userRepo, personaRepo, encoder);

    @Test
    void createsTheAppAccountsWithTheDemoPassword() {
        when(encoder.encode("demo123")).thenReturn("hash-demo");
        when(personaRepo.save(any(Persona.class))).thenAnswer(i -> i.getArgument(0));

        carga.run(null);

        // 8 usuarios finales, 4 agentes activos y 7 ejecutivos con la misma clave
        ArgumentCaptor<Users> cuentas = ArgumentCaptor.forClass(Users.class);
        verify(userRepo, times(19)).save(cuentas.capture());
        List<Users> guardadas = cuentas.getAllValues();
        assertThat(guardadas).allMatch(u -> u.getPassword().equals("hash-demo") && u.getPersona() != null);
        assertThat(guardadas).filteredOn(u -> u.getRole() == Role.EJECUTIVO).hasSize(7);
        assertThat(guardadas).filteredOn(u -> u.getRole() == Role.AGENTE).hasSize(4);
        assertThat(guardadas).extracting(Users::getEmail).contains("crojas@correo.ejemplo", "psaavedra@altosmaipo.ejemplo");
        verify(encoder, times(1)).encode(anyString());
    }

    @Test
    void doesNotRepeatExistingAccounts() {
        when(userRepo.existsByEmail(anyString())).thenReturn(true);

        carga.run(null);

        verify(userRepo, never()).save(any());
        verifyNoInteractions(personaRepo);
    }
}
