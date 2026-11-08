package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.security.CurrentUser;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.dto.request.VerifyCurrentPasswordRequest;
import com.pagoflex.usuarios.features.password.dto.response.VerifyCurrentPasswordResponse;
import com.pagoflex.usuarios.features.password.exceptions.AuthenticatedUserNotFound;
import com.pagoflex.usuarios.features.password.exceptions.InvalidChangeToken;
import com.pagoflex.usuarios.features.password.exceptions.InvalidCurrentPassword;
import com.pagoflex.usuarios.features.password.exceptions.PasswordReuseNotAllowed;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordServiceImpl implements PasswordService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final ChangeTokenService changeTokenService;


    @Override
    public VerifyCurrentPasswordResponse verifyCurrentPassword(
            VerifyCurrentPasswordRequest request
    ) {
        // 1. obtener el usuario autenticado
        Users user = userRepo.findById(CurrentUser.id())
                .orElseThrow(AuthenticatedUserNotFound::new);

        // 2. comprobar la contraseña actual
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCurrentPassword();
        }

        // 3. generar y almacenar un token temporal
        String token = changeTokenService.issueToken(
                user.getId(),
                user.getPassword()
        );

        // 4. devolver el token y su expiración
        return new VerifyCurrentPasswordResponse(
                token,
                changeTokenService.expiresInSeconds()
        );
    }


    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // 1. obtener y bloquear la cuenta durante el cambio
        Users user = userRepo.findByIdForUpdate(CurrentUser.id())
                .orElseThrow(AuthenticatedUserNotFound::new);

        // 2. impedir reutilizar la contraseña vigente
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new PasswordReuseNotAllowed();
        }

        // 3. validar y consumir el token temporal
        boolean valid = changeTokenService.consumeToken(
                user.getId(),
                request.changeToken(),
                user.getPassword()
        );

        if (!valid) {
            throw new InvalidChangeToken();
        }

        // 4. actualizar el hash de contraseña
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }
}