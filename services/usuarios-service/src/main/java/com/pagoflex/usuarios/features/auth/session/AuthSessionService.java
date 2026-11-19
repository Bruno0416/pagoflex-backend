package com.pagoflex.usuarios.features.auth.session;

import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;
import com.pagoflex.usuarios.features.auth.entity.Users;
import java.util.UUID;

public interface AuthSessionService {
    AuthResponse issue(Users user);
    AuthResponse refresh(String token);
    AuthResponse replaceCurrent(Users user);
    void revokeCurrent();
    void revokeAll(UUID userId);
}
