package com.pagoflex.usuarios.features.auth.service;

import com.pagoflex.usuarios.core.security.JwtUtil;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidCredentials;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserRepository userRepo;
    @Mock
    private PersonaRepository personaRepo;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private AuthServiceImpl service;

    // ---------- registro ----------
    @Test
    void registerNormalizesEmailAndEncodesPassword() {
        when(encoder.encode("Clave123!")).thenReturn("hash-seguro");

        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99.999.999-9", "Usuario", "Prueba"));

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).existsByEmail("usuario@example.com");
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("usuario@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-seguro");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USUARIO_FINAL);

        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepo).save(personaCaptor.capture());
        assertThat(personaCaptor.getValue().getRut()).isEqualTo("99999999-9");
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepo.existsByEmail("usuario@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba")))
                .isInstanceOf(EmailAlreadyInUse.class);
        verify(userRepo, never()).save(any());
        verifyNoInteractions(personaRepo);
    }

    @Test
    void registerRejectsInvalidRutBeforeSavingAnything() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-8", "Usuario", "Prueba")))
                .isInstanceOf(InvalidRut.class);
        verifyNoInteractions(userRepo, personaRepo, encoder);
    }

    // ---------- inicio de sesión ----------
    @Test
    void loginAuthenticatesAndReturnsJwt() {
        Users user = Users.builder().email("usuario@example.com").build();
        when(userRepo.findByEmail("usuario@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user)).thenReturn("jwt-de-prueba");

        var response = service.login(new LoginRequest(" USUARIO@example.com ", "contrasena123"));

        assertThat(response.token()).isEqualTo("jwt-de-prueba");
        verify(authManager).authenticate(argThat(auth ->
                "usuario@example.com".equals(auth.getPrincipal()) &&
                        "contrasena123".equals(auth.getCredentials())));
    }

    @Test
    void loginRejectsInvalidCredentials() {
        when(userRepo.findByEmail("usuario@example.com"))
                .thenReturn(Optional.of(Users.builder().email("usuario@example.com").build()));
        when(authManager.authenticate(any())).thenThrow(new BadCredentialsException("incorrecta"));

        assertThatThrownBy(() -> service.login(new LoginRequest("usuario@example.com", "incorrecta")))
                .isInstanceOf(InvalidCredentials.class);
        verifyNoInteractions(jwtUtil);
    }

}
