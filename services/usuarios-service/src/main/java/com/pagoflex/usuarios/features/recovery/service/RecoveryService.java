package com.pagoflex.usuarios.features.recovery.service;

import com.pagoflex.usuarios.features.recovery.dto.request.RecoveryRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.ResetPasswordRequest;
import com.pagoflex.usuarios.features.recovery.dto.request.VerifyCodeRequest;
import com.pagoflex.usuarios.features.recovery.dto.response.VerifyResetTokenResponse;

public interface RecoveryService {
    void requestPasswordReset(RecoveryRequest request);

    VerifyResetTokenResponse verifyResetCode(VerifyCodeRequest request);

    void resendCode(RecoveryRequest request);

    void resetPassword(ResetPasswordRequest request);
}
