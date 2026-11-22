package com.pagoflex.usuarios.features.recovery.service;

import com.pagoflex.contracts.correo.TipoCodigo;
import com.pagoflex.contracts.correo.TipoCorreo;
import com.pagoflex.contracts.correo.SolicitudEnvioCorreo;
import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.usuarios.core.client.EmailServiceClient;
import com.pagoflex.usuarios.core.exceptions.UserNotFound;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;
import com.pagoflex.usuarios.features.recovery.service.code.VerificationCodeService;
import com.pagoflex.usuarios.features.recovery.service.code.RecoverySendThrottle;
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
    private final AuthSessionService sessions;
    private final RecoverySendThrottle sendThrottle;

    // ---------- solicitar recovery ----------
    @Override
    public void requestPasswordReset(RecoveryRequest request) {
        String email = EmailUtils.normalize(request.correo());
        // 1. evitar revelar si el correo pertenece a una cuenta

        userRepo.findByEmail(email).ifPresent(user -> {
            // La misma ventana de 60 s limita tanto la solicitud como el reenvio.
            if (!sendThrottle.allow(email)) return;
            // 2. generar el codigo y solicitar su envio
            String code = verifCodeService.generateCode(email, TipoCodigo.RECOVERY);
            emailServiceClient.send(new SolicitudEnvioCorreo(email, TipoCorreo.RECOVERY, Map.of("code", code)));
        });
    }

    // ---------- verificar codigo ----------
    @Override
    public VerifyResetTokenResponse verifyResetCode(VerifyCodeRequest request) {
        // 1. normalizar el correo
        String email = EmailUtils.normalize(request.correo());

        // 2. verificar el codigo y generar la credencial temporal
        String temporaryCredential = verifCodeService.verifyCodeAndIssueResetToken(
                email, TipoCodigo.RECOVERY, request.codigo()
        );

        // 3. devolver la credencial de recuperacion
        return new VerifyResetTokenResponse(temporaryCredential);
    }

    // ---------- reenviar codigo ----------
    @Override
    public void resendCode(RecoveryRequest request) {
        String email = EmailUtils.normalize(request.correo());
        // 1. evitar revelar si el correo pertenece a una cuenta

        userRepo.findByEmail(email).ifPresent(user -> {
            // La misma ventana de 60 s limita tanto la solicitud como el reenvio.
            if (!sendThrottle.allow(email)) return;
            // 2. generar el codigo y solicitar su envio
            String code = verifCodeService.generateCode(email, TipoCodigo.RECOVERY);
            emailServiceClient.send(new SolicitudEnvioCorreo(email, TipoCorreo.RECOVERY, Map.of("code", code)));
        });
    }


    // ---------- cambiar contrasena ----------
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        // 1. consumir la credencial temporal de un solo uso
        String email = verifCodeService.consumeResetToken(request.tokenRestablecimiento());

        // 2. encontrar el usuario asociado
        Users user = userRepo.findByEmail(email).orElseThrow(() -> new UserNotFound(email));

        // 3. hashear y guardar la nueva contrasena
        user.setPassword(passwordEncoder.encode(request.nuevaContrasena()));

        // 4. revocar sesiones antes de confirmar las nuevas credenciales
        sessions.revokeAll(user.getId());
        userRepo.save(user);
    }
}
