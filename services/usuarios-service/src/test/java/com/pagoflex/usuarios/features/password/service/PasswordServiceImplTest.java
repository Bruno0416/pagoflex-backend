package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.core.error.PagoFlexException;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.dto.request.VerifyCurrentPasswordRequest;
import com.pagoflex.usuarios.features.password.exceptions.AuthenticatedUserNotFound;
import com.pagoflex.usuarios.features.password.exceptions.InvalidChangeToken;
import com.pagoflex.usuarios.features.password.exceptions.InvalidCurrentPassword;
import com.pagoflex.usuarios.features.password.exceptions.PasswordReuseNotAllowed;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordServiceImplTest {
    private static final UUID USER_ID = UUID.fromString("12a7be82-ecaa-490d-a69b-5d6b1edacbb7");

    @Mock
    private UserRepository userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ChangeTokenService changeTokenService;

    @InjectMocks
    private PasswordServiceImpl service;

    @BeforeEach
    void authenticateUser() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "HS256")
                .subject(USER_ID.toString())
                .claim("userId", USER_ID.toString())
                .claim("email", "usuario@example.com")
                .claim("roles", List.of("USUARIO_FINAL"))
                .claim("token_type", "ACCESS")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_USUARIO_FINAL")))
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ---------- verificar contrasena actual ----------
    @Test
    void verifyRejectsUnknownAuthenticatedUser() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyCurrentPassword(new VerifyCurrentPasswordRequest("clave")))
                .isInstanceOf(AuthenticatedUserNotFound.class)
                .isInstanceOf(PagoFlexException.class)
                .satisfies(ex -> assertThat(((PagoFlexException) ex).getStatus()).isEqualTo(401));
        verifyNoInteractions(passwordEncoder, changeTokenService);
    }

    @Test
    void verifyRejectsIncorrectCurrentPassword() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user("hash-actual")));
        when(passwordEncoder.matches("incorrecta", "hash-actual")).thenReturn(false);

        assertThatThrownBy(() -> service.verifyCurrentPassword(new VerifyCurrentPasswordRequest("incorrecta")))
                .isInstanceOf(InvalidCurrentPassword.class)
                .isInstanceOf(PagoFlexException.class)
                .satisfies(ex -> assertThat(((PagoFlexException) ex).getStatus()).isEqualTo(400));
        verifyNoInteractions(changeTokenService);
    }

    @Test
    void verifyReturnsTokenWhenPasswordMatches() {
        when(userRepo.findById(USER_ID)).thenReturn(Optional.of(user("hash-actual")));
        when(passwordEncoder.matches("correcta", "hash-actual")).thenReturn(true);
        when(changeTokenService.issueToken(USER_ID, "hash-actual")).thenReturn("token-temporal");
        when(changeTokenService.expiresInSeconds()).thenReturn(300L);

        var response = service.verifyCurrentPassword(new VerifyCurrentPasswordRequest("correcta"));

        assertThat(response.changeToken()).isEqualTo("token-temporal");
        assertThat(response.expiresIn()).isEqualTo(300L);
    }

    // ---------- cambiar contrasena ----------
    @Test
    void changeRejectsUnknownAuthenticatedUser() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("token", "Nueva123!")))
                .isInstanceOf(AuthenticatedUserNotFound.class)
                .satisfies(ex -> assertThat(((PagoFlexException) ex).getStatus()).isEqualTo(401));
        verifyNoInteractions(passwordEncoder, changeTokenService);
    }

    @Test
    void changeRejectsSamePassword() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user("hash-actual")));
        when(passwordEncoder.matches("Nueva123!", "hash-actual")).thenReturn(true);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("token", "Nueva123!")))
                .isInstanceOf(PasswordReuseNotAllowed.class)
                .satisfies(ex -> assertThat(((PagoFlexException) ex).getStatus()).isEqualTo(400));
        verifyNoInteractions(changeTokenService);
    }

    @Test
    void changeRejectsInvalidToken() {
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user("hash-actual")));
        when(changeTokenService.consumeToken(USER_ID, "token-invalido", "hash-actual")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(new ChangePasswordRequest("token-invalido", "Nueva123!")))
                .isInstanceOf(InvalidChangeToken.class)
                .satisfies(ex -> assertThat(((PagoFlexException) ex).getStatus()).isEqualTo(400));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changeUpdatesHashWithValidToken() {
        Users user = user("hash-actual");
        when(userRepo.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(changeTokenService.consumeToken(USER_ID, "token-valido", "hash-actual")).thenReturn(true);
        when(passwordEncoder.encode("Nueva123!")).thenReturn("hash-nuevo");

        service.changePassword(new ChangePasswordRequest("token-valido", "Nueva123!"));

        assertThat(user.getPassword()).isEqualTo("hash-nuevo");
    }

    // --- helpers privados ---
    private Users user(String hash) {
        return Users.builder().id(USER_ID).password(hash).build();
    }
}
