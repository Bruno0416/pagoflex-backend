package com.pagoflex.usuarios.features.auth.service;

import com.pagoflex.core.utils.EmailUtils;
import com.pagoflex.core.utils.RutUtils;
import com.pagoflex.usuarios.core.security.JwtUtil;
import com.pagoflex.usuarios.core.security.Role;
import com.pagoflex.usuarios.features.auth.dto.request.LoginRequest;
import com.pagoflex.usuarios.features.auth.dto.request.RegisterRequest;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import com.pagoflex.usuarios.features.auth.exceptions.EmailAlreadyInUse;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidCredentials;
import com.pagoflex.usuarios.features.auth.exceptions.InvalidRut;
import com.pagoflex.usuarios.features.auth.repository.UserRepository;
import com.pagoflex.usuarios.features.profile.entity.Persona;
import com.pagoflex.usuarios.features.profile.repository.PersonaRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository userRepo;
    private final PersonaRepository personaRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;

    // ---------- registro ----------
    @Override
    @Transactional
    public void register(RegisterRequest request) {
        // 1. normalizar el correo y rut
        String email = EmailUtils.normalize(request.email());
        String rut;
        try {
            rut = RutUtils.normalize(request.rut());
        } catch (IllegalArgumentException ex) {
            throw new InvalidRut();
        }

        // 2. rechazar correos ya registrados
        if (userRepo.existsByEmail(email)) throw new EmailAlreadyInUse(email);

        // 3. guardar el usuario con la contraseña hasheada
        userRepo.save(Users.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(Role.USUARIO_FINAL)
                .build());

        // 4. registrar perfil del usuario
        personaRepo.save(
                Persona.builder()
                        .rut(rut)
                        .nombres(request.nombres())
                        .apellidos(request.apellidos())
                        .correoContacto(null)
                        .telefono(null)
                        .build()
        );
    }

    // ---------- login ----------
    @Override
    public AuthResponse login(LoginRequest request) {
        // 1. obtener la cuenta por correo
        String email = EmailUtils.normalize(request.email());
        Users user = userRepo.findByEmail(email)
                .orElseThrow(InvalidCredentials::new);

        // 2. validar las credenciales
        try {
            authManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (org.springframework.security.core.AuthenticationException ex) {

            // 2. rechazar las credenciales inválidas
            throw new InvalidCredentials();
        }

        // 3. emitir el unico jwt de acceso
        return new AuthResponse(jwtUtil.generateToken(user));
    }
}
