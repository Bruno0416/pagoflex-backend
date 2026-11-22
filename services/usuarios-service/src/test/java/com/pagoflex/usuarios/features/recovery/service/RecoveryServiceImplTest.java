package com.pagoflex.usuarios.features.recovery.service;

import com.pagoflex.contracts.correo.TipoCodigo;
import com.pagoflex.contracts.correo.TipoCorreo;
import com.pagoflex.contracts.correo.SolicitudEnvioCorreo;
import com.pagoflex.usuarios.core.client.EmailServiceClient;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import com.pagoflex.usuarios.features.recovery.service.code.VerificationCodeService;
import com.pagoflex.usuarios.features.recovery.service.code.RecoverySendThrottle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecoveryServiceImplTest {
    @Mock
    private UserRepository userRepo;
    @Mock
    private VerificationCodeService codeService;
    @Mock
    private PasswordEncoder encoder;
    @Mock
    private EmailServiceClient emailClient;

    @Mock
    private AuthSessionService sessions;
    @Mock
    private RecoverySendThrottle sendThrottle;
    @InjectMocks
    private RecoveryServiceImpl service;

    // ---------- recuperacion ----------
    @Test
    void recoveryDoesNotExposeUnknownAccountsOrSendEmails() {
        service.requestPasswordReset(new RecoveryRequest("desconocido@example.com"));

        verify(userRepo).findByEmail("desconocido@example.com");
        verifyNoInteractions(codeService, emailClient);
    }

    @Test
    void recoveryGeneratesAndSendsCodeForExistingAccount() {
        when(userRepo.findByEmail("usuario@example.com"))
                .thenReturn(Optional.of(Users.builder().email("usuario@example.com").build()));
        when(sendThrottle.allow("usuario@example.com")).thenReturn(true);
        when(codeService.generateCode("usuario@example.com", TipoCodigo.RECOVERY)).thenReturn("ABC123");

        service.requestPasswordReset(new RecoveryRequest(" USUARIO@EXAMPLE.COM "));

        verify(emailClient).send(new SolicitudEnvioCorreo("usuario@example.com", TipoCorreo.RECOVERY, Map.of("code", "ABC123")));
    }

    @Test
    void resendGeneratesNewCodeForExistingAccount() {
        when(userRepo.findByEmail("usuario@example.com"))
                .thenReturn(Optional.of(Users.builder().email("usuario@example.com").build()));
        when(sendThrottle.allow("usuario@example.com")).thenReturn(true);
        when(codeService.generateCode("usuario@example.com", TipoCodigo.RECOVERY)).thenReturn("XYZ789");

        service.resendCode(new RecoveryRequest("usuario@example.com"));

        verify(emailClient).send(new SolicitudEnvioCorreo("usuario@example.com", TipoCorreo.RECOVERY, Map.of("code", "XYZ789")));
    }

    @Test
    void recoveryCooldownKeepsPublicResponseWithoutSendingAnotherCode() {
        when(userRepo.findByEmail("usuario@example.com"))
                .thenReturn(Optional.of(Users.builder().email("usuario@example.com").build()));
        when(sendThrottle.allow("usuario@example.com")).thenReturn(false);

        service.resendCode(new RecoveryRequest("usuario@example.com"));

        verifyNoInteractions(codeService, emailClient);
    }

    @Test
    void verifyDelegatesToRedisAndReturnsTemporaryToken() {
        when(codeService.verifyCodeAndIssueResetToken("usuario@example.com", TipoCodigo.RECOVERY, "ABC123"))
                .thenReturn("token-temporal");

        var response = service.verifyResetCode(new VerifyCodeRequest(" USUARIO@example.com ", "ABC123"));

        assertThat(response.tokenRestablecimiento()).isEqualTo("token-temporal");
    }

    @Test
    void resetChangesPasswordAfterConsumingToken() {
        Users user = Users.builder().email("usuario@example.com").password("hash-anterior").build();
        when(codeService.consumeResetToken("token-temporal")).thenReturn("usuario@example.com");
        when(userRepo.findByEmail("usuario@example.com")).thenReturn(Optional.of(user));
        when(encoder.encode("NuevaClave123!")).thenReturn("hash-nuevo");

        service.resetPassword(new ResetPasswordRequest("token-temporal", "NuevaClave123!"));

        assertThat(user.getPassword()).isEqualTo("hash-nuevo");
        verify(userRepo).save(user);
        verify(sessions).revokeAll(user.getId());
    }

    @Test
    void resetRejectsUsedTokenWithoutChangingUser() {
        when(codeService.consumeResetToken("token-usado")).thenThrow(new InvalidResetToken());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("token-usado", "NuevaClave123!")))
                .isInstanceOf(InvalidResetToken.class);
        verifyNoInteractions(userRepo, encoder);
    }
}
