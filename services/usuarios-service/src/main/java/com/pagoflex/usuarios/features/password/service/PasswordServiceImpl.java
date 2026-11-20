package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.core.error.FieldErrorException;
import com.pagoflex.security.CurrentUser;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.exceptions.AuthenticatedUserNotFound;
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
    private final AuthSessionService sessions;

    // ---------- cambio de contrasena desde perfil ----------
    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        // 1. obtener la cuenta autenticada y bloquear cambios simultaneos
        Users user = userRepo.findByIdForUpdate(CurrentUser.id())
                .orElseThrow(AuthenticatedUserNotFound::new);

        // 2. verificar la contrasena actual; nunca confiar solo en el jwt
        if (!passwordEncoder.matches(request.contrasenaActual(), user.getPassword())) {
            throw new InvalidCurrentPassword();
        }

        // 3. exigir confirmacion de la nueva contrasena
        if (!request.nuevaContrasena().equals(request.confirmacionContrasena())) {
            throw new FieldErrorException(400, "confirmacionContrasena", "Las contrasenas no coinciden");
        }

        // 4. impedir reutilizar la contrasena vigente
        if (passwordEncoder.matches(request.nuevaContrasena(), user.getPassword())) {
            throw new PasswordReuseNotAllowed();
        }

        // 5. guardar el hash nuevo y revocar las sesiones
        user.setPassword(passwordEncoder.encode(request.nuevaContrasena()));
        userRepo.flush();
        sessions.revokeAll(user.getId());
    }
}
