package com.pagoflex.usuarios.features.password.service;

import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;

public interface PasswordService {
    void changePassword(ChangePasswordRequest request);
}
