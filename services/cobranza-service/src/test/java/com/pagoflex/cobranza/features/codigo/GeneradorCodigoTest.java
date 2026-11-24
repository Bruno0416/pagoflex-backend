package com.pagoflex.cobranza.features.codigo;

import com.pagoflex.cobranza.features.codigo.service.GeneradorCodigo;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorCodigoTest {

    @Test
    void elCodigoGeneradoCumpleElFormatoDelContrato() {
        for (int i = 0; i < 200; i++) {
            String codigo = GeneradorCodigo.codigoAleatorio();
            assertThat(codigo).hasSize(6);
            assertThat(SolicitudValidacionCodigoEmpresa.esValido(codigo)).isTrue();
        }
    }

    @Test
    void referenciaOpacaEstableYSeparadaPorTipo() {
        String codigo = "AB12CD";
        String clave = "clave-test";
        String referencia = GeneradorCodigo.referencia(codigo, clave, false);
        assertThat(referencia).matches("R[A-Z2-7]{5}").isNotEqualTo(codigo);
        assertThat(GeneradorCodigo.referencia(codigo, clave, false)).isEqualTo(referencia);
        assertThat(GeneradorCodigo.referencia(codigo, clave, true)).matches("D[A-Z2-7]{5}");
        assertThat(GeneradorCodigo.referencia(codigo, "otra-clave", false)).isNotEqualTo(referencia);
    }
}
