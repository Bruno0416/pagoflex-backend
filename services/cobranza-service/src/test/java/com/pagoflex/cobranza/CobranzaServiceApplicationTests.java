package com.pagoflex.cobranza;

import com.pagoflex.cobranza.features.compromiso.data.CargaDemo;
import com.pagoflex.cobranza.features.empresa.data.CargaEmpresas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CobranzaServiceApplicationTests {
    @Autowired
    private ApplicationContext contexto;

    // ---------- comprobar el arranque del contexto ----------
    @Test
    void contextLoads() {
    }

    // con pagoflex.demo.enabled sin activar ninguna carga de ejemplo existe ni se ejecuta
    @Test
    void sinElFlagDemoNoHayCargasDeEjemplo() {
        assertThat(contexto.getBeanNamesForType(CargaDemo.class)).isEmpty();
        assertThat(contexto.getBeanNamesForType(CargaEmpresas.class)).isEmpty();
    }
}
