package com.pagoflex.cobranza.features.codigo.service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

public final class GeneradorCodigo {
    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorCodigo() {
    }

    public static String codigoAleatorio() {
        return "" + letra() + letra() + RANDOM.nextInt(10) + RANDOM.nextInt(10) + letra() + letra();
    }

    // referencia opaca de busqueda, separada de los codigos normales y demo
    public static String referencia(String codigo, String clave, boolean demo) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(clave.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(("pagoflex:codigo:" + (demo ? "demo:" : "normal:") + codigo)
                    .getBytes(StandardCharsets.UTF_8));
            // se conserva la columna existente de seis caracteres; bcrypt verifica el codigo completo
            int bits = ((hash[0] & 255) << 17) | ((hash[1] & 255) << 9)
                    | ((hash[2] & 255) << 1) | ((hash[3] & 255) >>> 7);
            StringBuilder referencia = new StringBuilder(demo ? "D" : "R");
            for (int i = 20; i >= 0; i -= 5) referencia.append(BASE32.charAt((bits >>> i) & 31));
            return referencia.toString();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("No se pudo calcular la referencia del codigo", ex);
        }
    }

    private static char letra() {
        return LETRAS.charAt(RANDOM.nextInt(LETRAS.length()));
    }
}
