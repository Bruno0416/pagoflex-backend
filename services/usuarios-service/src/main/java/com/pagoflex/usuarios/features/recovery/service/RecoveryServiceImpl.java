package com.pagoflex.usuarios.features.recovery.service;

import com.pagoflex.contracts.email.CodeType;
import com.pagoflex.contracts.email.EmailType;
import com.pagoflex.contracts.email.SendEmailRequest;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.usuarios.core.client.EmailServiceClient;
import com.pagoflex.usuarios.core.exceptions.UserNotFound;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResendRecoveryCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;
import com.pagoflex.usuarios.features.recovery.service.code.VerificationCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecoveryServiceImpl implements RecoveryService {
    private final UserRepository userRepo;
    private final VerificationCodeService verifCodeService;
    private final PasswordEncoder passwordEncoder;
    private final EmailServiceClient emailServiceClient;

    // ---------- solicitar recovery ----------
    @Override
    public void requestPasswordReset(RecoveryRequest request) {
        String email = EmailUtils.normalize(request.email());
        // 1. evitar revelar si el correo pertenece a una cuenta

        userRepo.findByEmail(email).ifPresent(user -> {
            // 2. generar el código y solicitar su envío
            String code = verifCodeService.generateCode(email, CodeType.RECOVERY);
            emailServiceClient.send(new SendEmailRequest(email, EmailType.RECOVERY, Map.of("code", code)));
        });
    }

    // ---------- verificar código ----------
    @Override
    public VerifyResetTokenResponse verifyResetCode(VerifyCodeRequest request) {
        // 1. normalizar el correo
        String email = EmailUtils.normalize(request.email());

        // 2. verificar el código y generar la credencial temporal
        String temporaryCredential = verifCodeService.verifyCodeAndIssueResetToken(
                email, CodeType.RECOVERY, request.code()
        );

        // 3. devolver la credencial de recuperación
        return new VerifyResetTokenResponse(temporaryCredential);
    }

    // ---------- reenviar código ----------
    @Override
    public void resendCode(ResendRecoveryCodeRequest request) {
        String email = EmailUtils.normalize(request.email());
        // 1. evitar revelar si el correo pertenece a una cuenta

        userRepo.findByEmail(email).ifPresent(user -> {
            // 2. generar el código y solicitar su envío
            String code = verifCodeService.generateCode(email, CodeType.RECOVERY);
            emailServiceClient.send(new SendEmailRequest(email, EmailType.RECOVERY, Map.of("code", code)));
        });
    }


    // ---------- cambiar contraseña ----------
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. consumir la credencial temporal de un solo uso
        String email = verifCodeService.consumeResetToken(request.resetToken());

        // 2. encontrar el usuario asociado
        Users user = userRepo.findByEmail(email).orElseThrow(() -> new UserNotFound(email));

        // 3. hashear y guardar la nueva contraseña
        user.setPassword(passwordEncoder.encode(request.newPassword()));

        userRepo.save(user);
    }
}
