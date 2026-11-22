package com.pagoflex.usuarios.features.recovery.service.code;

import com.pagoflex.contracts.correo.TipoCodigo;

public interface VerificationCodeService {
    String generateCode(String email, TipoCodigo type);

    String verifyCodeAndIssueResetToken(String email, TipoCodigo type, String code);

    String consumeResetToken(String resetToken);
}
