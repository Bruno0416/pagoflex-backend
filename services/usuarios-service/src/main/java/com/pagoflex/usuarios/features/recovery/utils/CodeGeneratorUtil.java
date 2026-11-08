package com.pagoflex.usuarios.features.recovery.utils;

import java.security.SecureRandom;

public class CodeGeneratorUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final String ALLOWED_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;

    public static String generateCode() {
        // 1. seleccionar caracteres aleatorios permitidos
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            int index = secureRandom.nextInt(ALLOWED_CHARS.length());
            code.append(ALLOWED_CHARS.charAt(index));
        }
        return code.toString();
    }
}
