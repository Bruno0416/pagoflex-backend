package com.pagoflex.usuarios.features.recovery.service.code;

import com.pagoflex.contracts.email.CodeType;

public interface VerificationCodeService {
    String generateCode(String email, CodeType type);

    String verifyCodeAndIssueResetToken(String email, CodeType type, String code);

    String consumeResetToken(String resetToken);
}
