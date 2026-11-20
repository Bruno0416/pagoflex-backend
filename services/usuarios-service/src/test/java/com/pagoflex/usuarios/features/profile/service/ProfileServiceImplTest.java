package com.pagoflex.usuarios.features.profile.service;

import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.exceptions.AvatarNotFound;
import com.pagoflex.usuarios.features.profile.exceptions.InvalidProfileField;
import com.pagoflex.usuarios.features.profile.storage.AvatarStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {
    private static final UUID USER_ID = UUID.fromString("12a7be82-ecaa-490d-a69b-5d6b1edacbb7");
    private final Users user = Users.builder()
            .id(USER_ID)
            .email("usuario@example.com")
            .password("hash")
            .role(Role.USUARIO_FINAL)
            .persona(Persona.builder().id(UUID.randomUUID()).rut("99999999-9")
                    .nombres("Usuario").apellidos("Prueba").build())
            .build();

    @Mock UserRepository userRepo;
    @Mock PasswordEncoder encoder;
    @Mock AuthSessionService sessions;
    @Mock AvatarStorage avatarStorage;
    @InjectMocks ProfileServiceImpl service;

    @BeforeEach
    void authenticate() {
        Jwt jwt = Jwt.withTokenValue("test-token").header("alg", "HS256")
                .subject(USER_ID.toString()).claim("roles", List.of("USUARIO_FINAL"))
                .claim("token_type", "ACCESS").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_FINAL"))));
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void cleanup() {
        TransactionSynchronizationManager.clearSynchronization();
        SecurityContextHolder.clearContext();
    }

    // ---------- consulta de perfil ----------
    @Test
    void getProfileReturnsLinkedPersona() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        var response = service.getCurrentProfile();
        assertThat(response.idPersona()).isEqualTo(user.getPersona().getId());
        assertThat(response.rut()).isEqualTo("99999999-9");
        assertThat(response.urlAvatar()).isNull();
    }

    // ---------- cambio de datos ----------
    @Test
    void updateNamesDoesNotRequirePasswordOrRotateJwt() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        var result = service.updateProfile(new UpdateProfileRequest("Nombre", "Nuevo", null, null));
        assertThat(result.perfil().nombres()).isEqualTo("Nombre");
        assertThat(result.perfil().telefono()).isNull();
        assertThat(result.sesion()).isNull();
        verifyNoInteractions(encoder, sessions);
    }

    @Test
    void changingEmailRequiresCorrectPassword() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileRequest(
                null, null, "nuevo@example.com", null)))
                .isInstanceOf(InvalidProfileField.class);
        verifyNoInteractions(sessions);
    }

    @Test
    void changingEmailRejectsDuplicates() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(encoder.matches("correcta", "hash")).thenReturn(true);
        when(userRepo.existsByEmail("nuevo@example.com")).thenReturn(true);
        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileRequest(
                null, null, "nuevo@example.com", "correcta")))
                .isInstanceOf(EmailAlreadyInUse.class);
        assertThat(user.getEmail()).isEqualTo("usuario@example.com");
    }

    @Test
    void changingEmailIssuesNewJwt() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(encoder.matches("correcta", "hash")).thenReturn(true);
        when(sessions.replaceCurrent(user))
                .thenReturn(new AuthResponse("jwt-nuevo", "refresh-nuevo", "Bearer", 900));
        var result = service.updateProfile(new UpdateProfileRequest(
                null, null, " NUEVO@example.com ", "correcta"));
        assertThat(user.getEmail()).isEqualTo("nuevo@example.com");
        assertThat(result.sesion().tokenAcceso()).isEqualTo("jwt-nuevo");
    }

    @Test
    void changingToBlankEmailReturnsFieldError() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        assertThatThrownBy(() -> service.updateProfile(new UpdateProfileRequest(
                null, null, "  ", null)))
                .isInstanceOf(InvalidProfileField.class);
    }

    // ---------- avatar ----------
    @Test
    void getAvatarReturnsNotFoundWhenMissing() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user));
        assertThatThrownBy(service::getAvatar).isInstanceOf(AvatarNotFound.class);
    }

    @Test
    void uploadDeletesOldAvatarAfterCommit() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        user.getPersona().setAvatar("anterior.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "nuevo.jpg", "image/jpeg", new byte[]{1});
        when(avatarStorage.save(file)).thenReturn("nuevo.jpg");
        assertThat(service.uploadAvatar(file).urlAvatar()).contains("nuevo.jpg");
        verify(avatarStorage, never()).delete(any());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(avatarStorage).delete("anterior.jpg");
    }

    @Test
    void uploadCleansNewAvatarOnRollback() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        MockMultipartFile file = new MockMultipartFile("file", "nuevo.jpg", "image/jpeg", new byte[]{1});
        when(avatarStorage.save(file)).thenReturn("nuevo.jpg");
        service.uploadAvatar(file);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(avatarStorage).delete("nuevo.jpg");
    }

    @Test
    void deleteAvatarKeepsFileUntilCommit() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        user.getPersona().setAvatar("anterior.jpg");
        service.deleteAvatar();
        assertThat(user.getPersona().getAvatar()).isNull();
        verifyNoInteractions(avatarStorage);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(avatarStorage).delete("anterior.jpg");
    }
}
