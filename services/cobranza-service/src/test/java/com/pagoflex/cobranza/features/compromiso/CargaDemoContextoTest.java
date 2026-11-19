package com.pagoflex.cobranza.features.compromiso;

import com.pagoflex.cobranza.core.client.UsuariosServiceClient;
import com.pagoflex.cobranza.features.compromiso.data.CargaDemo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;

// con los datos demo activos el servicio igual arranca aunque usuarios no responda
@SpringBootTest(properties = "pagoflex.demo.enabled=true")
class CargaDemoContextoTest {
    @Autowired
    private CargaDemo carga;

    @MockitoBean
    private UsuariosServiceClient usuarios;

    @Test
    void laCargaDemoSeCreaComoBean() {
        assertThat(carga).isNotNull();
    }
}
