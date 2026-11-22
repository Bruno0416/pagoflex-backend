package com.pagoflex.usuarios.features.profile.service;

import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.security.CurrentUser;
import com.pagoflex.usuarios.features.auth.session.AuthSessionService;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.dto.request.UpdateProfileRequest;
import com.pagoflex.usuarios.features.profile.dto.response.ProfileResponse;
import com.pagoflex.usuarios.features.profile.dto.response.UpdateProfileResponse;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.exceptions.AvatarNotFound;
import com.pagoflex.usuarios.features.profile.exceptions.InvalidProfileField;
import com.pagoflex.usuarios.features.profile.exceptions.ProfileNotFound;
import com.pagoflex.usuarios.features.profile.storage.AvatarStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.dao.DataIntegrityViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private static final Logger LOG = LoggerFactory.getLogger(ProfileServiceImpl.class);
    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthSessionService sessions;
    private final AvatarStorage avatarStorage;

    // ---------- consulta del perfil ----------
    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getCurrentProfile() {
        return toResponse(currentUser(false));
    }

    // ---------- edicion del perfil ----------
    @Override
    @Transactional
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        // 1. bloquear la cuenta para evitar cambios simultaneos
        Users user = currentUser(true);
        Persona persona = user.getPersona();

        // 2. editar datos personales si fueron enviados
        if (request.nombres() != null) persona.setNombres(request.nombres().trim());
        if (request.apellidos() != null) persona.setApellidos(request.apellidos().trim());

        // 3. comprobar la contrasena actual solo si cambia el correo
        AuthResponse sesion = null;
        if (request.correo() != null) {
            String email = EmailUtils.normalize(request.correo());
            if (email == null || email.isBlank()) {
                throw new InvalidProfileField("correo", "El correo no puede estar vacío");
            }
            if (!Objects.equals(email, user.getEmail())) {
                if (request.contrasenaActual() == null || request.contrasenaActual().isBlank() ||
                        !passwordEncoder.matches(request.contrasenaActual(), user.getPassword())) {
                    throw new InvalidProfileField("contrasenaActual", "La contraseña actual es incorrecta");
                }
                if (userRepo.existsByEmail(email)) throw new EmailAlreadyInUse(email);
                user.setEmail(email);
                // 4. forzar la validacion de unicidad antes de emitir el jwt
                try {
                    userRepo.flush();
                } catch (DataIntegrityViolationException ex) {
                    throw new EmailAlreadyInUse(email);
                }
                // 5. emitir un jwt con el correo actualizado
                sesion = sessions.replaceCurrent(user);
            }
        }
        return new UpdateProfileResponse(toResponse(user), sesion);
    }

    // ---------- gestion del avatar ----------
    @Override
    @Transactional(readOnly = true)
    public byte[] getAvatar() {
        String filename = currentUser(false).getPersona().getAvatar();
        if (filename == null) throw new AvatarNotFound();
        return avatarStorage.read(filename);
    }

    @Override
    @Transactional
    public ProfileResponse uploadAvatar(MultipartFile file) {
        // 1. bloquear el perfil y guardar el avatar nuevo
        Users user = currentUser(true);
        String newFilename = avatarStorage.save(file);
        String previousFilename = user.getPersona().getAvatar();
        user.getPersona().setAvatar(newFilename);

        // 2. conservar el archivo previo hasta confirmar mysql
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            cleanupAvatar(newFilename);
            throw new IllegalStateException("La operacion de avatar requiere una transaccion");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (previousFilename != null) cleanupAvatar(previousFilename);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) cleanupAvatar(newFilename);
            }
        });
        return toResponse(user);
    }

    @Override
    @Transactional
    public void deleteAvatar() {
        // 1. quitar la referencia del avatar actual
        Users user = currentUser(true);
        String previousFilename = user.getPersona().getAvatar();
        if (previousFilename == null) return;
        user.getPersona().setAvatar(null);

        // 2. eliminar el archivo solamente despues del commit
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                cleanupAvatar(previousFilename);
            }
        });
    }

    // --- helpers ---
    private Users currentUser(boolean lock) {
        Users user = (lock ? userRepo.findByIdForUpdate(CurrentUser.id()) : userRepo.findById(CurrentUser.id()))
                .orElseThrow(ProfileNotFound::new);
        if (user.getPersona() == null) throw new ProfileNotFound();
        return user;
    }

    private void cleanupAvatar(String filename) {
        try {
            avatarStorage.delete(filename);
        } catch (RuntimeException ex) {
            LOG.warn("No se pudo limpiar el avatar {}", filename, ex);
        }
    }

    private ProfileResponse toResponse(Users user) {
        Persona persona = user.getPersona();
        String avatar = persona.getAvatar();
        return new ProfileResponse(
                user.getId(), persona.getId(), persona.getRut(), persona.getNombres(), persona.getApellidos(),
                user.getEmail(), persona.getTelefono(), user.getRole().name(),
                avatar == null ? null : "/api/v1/usuarios/me/avatar?v=" + avatar, persona.getEmpresaId()
        );
    }
}
