package com.pagoflex.usuarios.features.password.service;


import com.pagoflex.usuarios.features.password.dto.request.ChangePasswordRequest;
import com.pagoflex.usuarios.features.password.dto.request.VerifyCurrentPasswordRequest;
import com.pagoflex.usuarios.features.password.dto.response.VerifyCurrentPasswordResponse;


public interface PasswordService {
    // 1. verificar current password y retornar token temporal
    // para validar cambio de contraseña
    VerifyCurrentPasswordResponse verifyCurrentPassword(VerifyCurrentPasswordRequest request);

    // 2. cambio de contraseña validado por token
    void changePassword(ChangePasswordRequest request);
}
