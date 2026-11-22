package com.pagoflex.mensajeria.features.conversacion.rules;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReglasConversacionTest {

    @Test
    void soloElAgenteAsignadoYElEjecutivoDeLaEmpresa() {
        UUID agente = UUID.randomUUID();
        UUID empresa = UUID.randomUUID();
        assertThat(ReglasConversacion.agentePuede(agente, agente)).isTrue();
        assertThat(ReglasConversacion.agentePuede(agente, UUID.randomUUID())).isFalse();
        assertThat(ReglasConversacion.agentePuede(null, agente)).isFalse();
        assertThat(ReglasConversacion.ejecutivoPuede(empresa, empresa)).isTrue();
        assertThat(ReglasConversacion.ejecutivoPuede(empresa, null)).isFalse();
    }

    @Test
    void resueltoEsSoloLecturaYElLimiteTieneTope() {
        assertThat(ReglasConversacion.soloLectura("RESUELTO")).isTrue();
        assertThat(ReglasConversacion.soloLectura("EN_ATENCION")).isFalse();
        assertThat(ReglasConversacion.limite(null)).isEqualTo(30);
        assertThat(ReglasConversacion.limite(0)).isEqualTo(30);
        assertThat(ReglasConversacion.limite(500)).isEqualTo(100);
    }
}
