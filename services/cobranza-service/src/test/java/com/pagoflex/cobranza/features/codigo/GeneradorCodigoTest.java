package com.pagoflex.cobranza.features.codigo;

import com.pagoflex.cobranza.features.codigo.service.GeneradorCodigo;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCodigoTest {

    @Test
    void elCodigoGeneradoCumpleElFormatoDelContrato() {
        for (int i = 0; i < 200; i++) {
            String codigo = GeneradorCodigo.armar(GeneradorCodigo.prefijoAleatorio(), GeneradorCodigo.secretoAleatorio());
            assertThat(codigo).hasSize(33);
            assertThat(SolicitudValidacionCodigoEmpresa.esValido(codigo)).isTrue();
            assertThat(GeneradorCodigo.prefijoDe(codigo)).matches("[A-Z]{2}[0-9]{2}[A-Z]{2}");
        }
    }

    @Test
    void elSecretoTiene26CaracteresBase32DeUn128BitsAleatorio() {
        Set<String> vistos = new HashSet<>();
        for (int i = 0; i < 500; i++) {
            String secreto = GeneradorCodigo.secretoAleatorio();
            assertThat(secreto).matches("[A-Z2-7]{26}");
            vistos.add(secreto);
        }
        // 128 bits: en 500 intentos no puede haber repetidos
        assertThat(vistos).hasSize(500);
    }

    @Test
    void base32SigueElEstandarRfc4648SinRelleno() {
        // vectores del RFC 4648 sin el relleno "="
        assertThat(GeneradorCodigo.base32("f".getBytes())).isEqualTo("MY");
        assertThat(GeneradorCodigo.base32("fo".getBytes())).isEqualTo("MZXQ");
        assertThat(GeneradorCodigo.base32("foo".getBytes())).isEqualTo("MZXW6");
        assertThat(GeneradorCodigo.base32("foobar".getBytes())).isEqualTo("MZXW6YTBOI");
        assertThat(GeneradorCodigo.base32(new byte[16])).isEqualTo("AAAAAAAAAAAAAAAAAAAAAAAAAA");
        assertThat(GeneradorCodigo.base32(new byte[]{-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1}))
                .hasSize(26).endsWith("4");
    }
}
