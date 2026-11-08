package com.pagoflex.usuarios.features.recovery.service;

import com.pagoflex.contracts.email.CodeType;
import com.pagoflex.contracts.email.EmailType;
import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.usuarios.core.client.EmailServiceClient;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResendRecoveryCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.exceptions.InvalidResetToken;
import com.pagoflex.usuarios.features.recovery.service.code.VerificationCodeService;
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
        when(codeService.generateCode("usuario@example.com", CodeType.RECOVERY)).thenReturn("ABC123");

        service.requestPasswordReset(new RecoveryRequest(" USUARIO@EXAMPLE.COM "));

        verify(emailClient).send(new SendEmailRequest("usuario@example.com", EmailType.RECOVERY, Map.of("code", "ABC123")));
    }

    @Test
    void resendGeneratesNewCodeForExistingAccount() {
        when(userRepo.findByEmail("usuario@example.com"))
                .thenReturn(Optional.of(Users.builder().email("usuario@example.com").build()));
        when(codeService.generateCode("usuario@example.com", CodeType.RECOVERY)).thenReturn("XYZ789");

        service.resendCode(new ResendRecoveryCodeRequest("usuario@example.com"));

        verify(emailClient).send(new SendEmailRequest("usuario@example.com", EmailType.RECOVERY, Map.of("code", "XYZ789")));
    }

    @Test
    void verifyDelegatesToRedisAndReturnsTemporaryToken() {
        when(codeService.verifyCodeAndIssueResetToken("usuario@example.com", CodeType.RECOVERY, "ABC123"))
                .thenReturn("token-temporal");

        var response = service.verifyResetCode(new VerifyCodeRequest(" USUARIO@example.com ", "ABC123"));

        assertThat(response.resetToken()).isEqualTo("token-temporal");
    }

    @Test
    void resetChangesPasswordAfterConsumingToken() {
        Users user = Users.builder().email("usuario@example.com").password("hash-anterior").build();
        when(codeService.consumeResetToken("token-temporal")).thenReturn("usuario@example.com");
        when(userRepo.findByEmail("usuario@example.com")).thenReturn(Optional.of(user));
        when(encoder.encode("nuevaClave123")).thenReturn("hash-nuevo");

        service.resetPassword(new ResetPasswordRequest("token-temporal", "nuevaClave123"));

        assertThat(user.getPassword()).isEqualTo("hash-nuevo");
        verify(userRepo).save(user);
    }

    @Test
    void resetRejectsUsedTokenWithoutChangingUser() {
        when(codeService.consumeResetToken("token-usado")).thenThrow(new InvalidResetToken());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("token-usado", "nuevaClave123")))
                .isInstanceOf(InvalidResetToken.class);
        verifyNoInteractions(userRepo, encoder);
    }
}
