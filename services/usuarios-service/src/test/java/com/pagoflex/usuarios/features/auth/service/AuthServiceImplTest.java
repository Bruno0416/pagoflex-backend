package com.pagoflex.usuarios.features.auth.service;

import com.pagoflex.usuarios.core.client.OrganizationClient;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.contracts.organization.*;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidOrganizationCode;
import com.pagoflex.usuarios.features.auth.exceptions.OrganizationServiceUnavailable;
import java.util.UUID;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidCredentials;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.exceptions.RutAlreadyInUse;
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
    private AuthSessionService sessions;
    @Mock
    private OrganizationClient organizationClient;
    @Mock
    private AuthenticationManager authManager;

    @InjectMocks
    private AuthServiceImpl service;

    // ---------- registro ----------
    @Test
    void registerNormalizesEmailAndEncodesPassword() {
        when(encoder.encode("Clave123!")).thenReturn("hash-seguro");
        when(personaRepo.saveAndFlush(any(Persona.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99.999.999-9", "Usuario", "Prueba", null));

        ArgumentCaptor<Users> captor = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).existsByEmail("usuario@example.com");
        verify(userRepo).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("usuario@example.com");
        assertThat(captor.getValue().getPassword()).isEqualTo("hash-seguro");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USUARIO_FINAL);
        assertThat(captor.getValue().getPersona()).isNotNull();

        ArgumentCaptor<Persona> personaCaptor = ArgumentCaptor.forClass(Persona.class);
        verify(personaRepo).saveAndFlush(personaCaptor.capture());
        assertThat(personaCaptor.getValue().getRut()).isEqualTo("99999999-9");
        verify(personaRepo).existsByRut("99999999-9"); // unicidad en formato persistido
        assertThat(captor.getValue().getPersona()).isSameAs(personaCaptor.getValue());
    }

    @Test
    void registerRejectsAnExistingEmail() {
        when(userRepo.existsByEmail("usuario@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null)))
                .isInstanceOf(EmailAlreadyInUse.class);
        verify(userRepo, never()).saveAndFlush(any());
        verifyNoInteractions(personaRepo);
    }

    @Test
    void registerRejectsDuplicateRutWithoutSavingAnything() {
        when(personaRepo.existsByRut("99999999-9")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", null)))
                .isInstanceOf(RutAlreadyInUse.class);
        verify(userRepo, never()).saveAndFlush(any());
        verify(personaRepo, never()).saveAndFlush(any());
        verifyNoInteractions(encoder);
    }

    @Test
    void registerRejectsInvalidRutBeforeSavingAnything() {
        assertThatThrownBy(() -> service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-8", "Usuario", "Prueba", null)))
                .isInstanceOf(InvalidRut.class);
        verifyNoInteractions(userRepo, personaRepo, encoder);
    }

    // ---------- inicio de sesion ----------
    @Test
    void loginAuthenticatesAndReturnsJwt() {
        Users user = Users.builder().email("usuario@example.com").build();
        when(userRepo.findByEmail("usuario@example.com")).thenReturn(Optional.of(user));
        when(sessions.issue(user)).thenReturn(new AuthResponse("jwt-de-prueba", "refresh-de-prueba", "Bearer", 900));

        var response = service.login(new LoginRequest(" USUARIO@example.com ", "contrasena123"));

        assertThat(response.tokenAcceso()).isEqualTo("jwt-de-prueba");
        assertThat(response.tokenRenovacion()).isEqualTo("refresh-de-prueba");
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
        verifyNoInteractions(sessions);
    }

    // ---------- registro con codigo de empresa ----------
    @Test
    void registerAgentWithValidInternalCode() {
        UUID empresa = UUID.randomUUID();
        when(organizationClient.validate("PF12AG"))
                .thenReturn(new ValidateOrganizationCodeResponse(empresa, "PagoFlex",
                        OrganizationType.INTERNA, AuthorizedRole.AGENTE));
        when(personaRepo.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));
        when(encoder.encode("Clave123!")).thenReturn("hash");

        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9",
                "Usuario", "Prueba", "PF12AG"));

        ArgumentCaptor<Users> user = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).saveAndFlush(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo(Role.AGENTE);
        assertThat(user.getValue().getPersona().getEmpresaId()).isEqualTo(empresa);
    }

    @Test
    void registerExecutiveWithValidClientCode() {
        UUID empresa = UUID.randomUUID();
        when(organizationClient.validate("CL34EX"))
                .thenReturn(new ValidateOrganizationCodeResponse(empresa, "Empresa",
                        OrganizationType.CLIENTE, AuthorizedRole.EJECUTIVO));
        when(personaRepo.saveAndFlush(any(Persona.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(new RegisterRequest("usuario@example.com", "Clave123!", "99999999-9",
                "Usuario", "Prueba", "CL34EX"));

        ArgumentCaptor<Users> user = ArgumentCaptor.forClass(Users.class);
        verify(userRepo).saveAndFlush(user.capture());
        assertThat(user.getValue().getRole()).isEqualTo(Role.EJECUTIVO);
        assertThat(user.getValue().getPersona().getEmpresaId()).isEqualTo(empresa);
    }

    @Test
    void rejectsCodeWithMismatchedRoleAndOrganization() {
        when(organizationClient.validate("CL56EX"))
                .thenReturn(new ValidateOrganizationCodeResponse(UUID.randomUUID(), "Empresa",
                        OrganizationType.CLIENTE, AuthorizedRole.AGENTE));
        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", "CL56EX")))
                .isInstanceOf(InvalidOrganizationCode.class);
        verify(userRepo, never()).saveAndFlush(any());
    }

    @Test
    void rejectsInvalidOrganizationCodeBeforeCallingCobranza() {
        // 1. comprobar formato, longitud y uso obligatorio de mayusculas
        for (String codigo : new String[]{"", " ", "AB12A", "AB123CD", "ABCDAB",
                "ab12ab", "AB12A1", "CODIGO-AGENTE"}) {
            assertThatThrownBy(() -> service.register(new RegisterRequest(
                    "usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", codigo)))
                    .isInstanceOf(InvalidOrganizationCode.class);
        }
        verifyNoInteractions(organizationClient);
        verify(userRepo, never()).saveAndFlush(any());
        verify(personaRepo, never()).saveAndFlush(any());
    }

    @Test
    void rejectsUnavailableCobranzaWithoutCreatingStaff() {
        when(organizationClient.validate("CL56EX"))
                .thenThrow(new OrganizationServiceUnavailable());
        assertThatThrownBy(() -> service.register(new RegisterRequest(
                "usuario@example.com", "Clave123!", "99999999-9", "Usuario", "Prueba", "CL56EX")))
                .isInstanceOf(OrganizationServiceUnavailable.class);
        verify(personaRepo, never()).saveAndFlush(any());
    }

    @Test
    void refreshDelegatesAndLogoutRevokesSession() {
        AuthResponse result = new AuthResponse("access", "refresh", "Bearer", 900);
        when(sessions.refresh("old-refresh")).thenReturn(result);
        assertThat(service.refresh("old-refresh")).isEqualTo(result);
        service.logout();
        verify(sessions).revokeCurrent();
    }

}
