package com.pagoflex.cobranza.features.codigo.service;

import java.security.SecureRandom;

/// Arma códigos AA00AA-XXXXXXXXXXXXXXXXXXXXXXXXXX: prefijo público + 128 bits aleatorios en Base32 (26 caracteres).
/// El alfabeto y la longitud son los mismos que valida ValidateOrganizationCodeRequest.
public final class GeneradorCodigo {
    private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final String LETRAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BYTES_SECRETO = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private GeneradorCodigo() {
    }

    // formato AA00AA: identificador público para buscar el registro; no es secreto
    public static String prefijoAleatorio() {
        return "" + letra() + letra() + RANDOM.nextInt(10) + RANDOM.nextInt(10) + letra() + letra();
    }

    public static String secretoAleatorio() {
        byte[] bytes = new byte[BYTES_SECRETO];
        RANDOM.nextBytes(bytes);
        return base32(bytes);
    }

    public static String armar(String prefijo, String secreto) {
        return prefijo + "-" + secreto;
    }

    public static String prefijoDe(String codigo) {
        return codigo.substring(0, 6);
    }

    // Base32 RFC 4648 sin relleno
    public static String base32(byte[] datos) {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bits = 0;
        for (byte b : datos) {
            buffer = (buffer << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32.charAt((buffer >> (bits - 5)) & 31));
                bits -= 5;
            }
            buffer &= (1 << bits) - 1;
        }
        if (bits > 0) sb.append(BASE32.charAt((buffer << (5 - bits)) & 31));
        return sb.toString();
    }

    private static char letra() {
        return LETRAS.charAt(RANDOM.nextInt(LETRAS.length()));
    }
}
