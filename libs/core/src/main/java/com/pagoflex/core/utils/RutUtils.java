package com.pagoflex.core.utils;

import java.util.Locale;
import java.util.regex.Pattern;

public final class RutUtils {
    private static final Pattern RUT_PATTERN = Pattern.compile("^\\d{1,8}[0-9K]$");

    public static boolean isValid(String rut) {
        // 1. rechazar valores vacíos
        if (rut == null || rut.isBlank()) {
            return false;
        }

        // 2. comprobar el formato limpio
        String cleaned = clean(rut);
        if (!RUT_PATTERN.matcher(cleaned).matches()) {
            return false;
        }

        // 3. calcular el dígito verificador mediante módulo 11
        int sum = 0;
        int multiplier = 2;
        for (int i = cleaned.length() - 2; i >= 0; i--) {
            sum += (cleaned.charAt(i) - '0') * multiplier;
            multiplier = multiplier == 7 ? 2 : multiplier + 1;
        }

        // 4. comparar el dígito calculado con el recibido
        int result = 11 - (sum % 11);
        char expected = result == 11 ? '0' : result == 10 ? 'K' : (char) ('0' + result);
        return cleaned.charAt(cleaned.length() - 1) == expected;
    }


    public static String normalize(String rut) {
        // 1. validar antes de formatear
        if (!isValid(rut)) {
            throw new IllegalArgumentException("El RUT no es válido");
        }

        // 2. separar el cuerpo del dígito verificador
        String cleaned = clean(rut);
        String body = cleaned.substring(0, cleaned.length() - 1);
        char verificationDigit = cleaned.charAt(cleaned.length() - 1);

        // 3. formato para persistencia: cuerpo-DV, sin puntos
        return body + "-" + verificationDigit;
    }

    // --- helper ---
    private static String clean(String rut) {
        return rut.trim().replace(".", "").replace("-", "").toUpperCase(Locale.ROOT);
    }
}
