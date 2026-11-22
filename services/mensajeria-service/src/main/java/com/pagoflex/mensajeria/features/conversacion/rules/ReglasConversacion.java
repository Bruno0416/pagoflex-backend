package com.pagoflex.mensajeria.features.conversacion.rules;

import com.pagoflex.mensajeria.features.conversacion.entity.RolParticipante;

import java.util.UUID;

/// Quién puede leer y escribir en la conversación de un reporte, sin base de datos
public final class ReglasConversacion {
    public static final int LIMITE_MAXIMO = 100;

    private ReglasConversacion() {
    }

    // solo el agente asignado al reporte
    public static boolean agentePuede(UUID agenteDelReporte, UUID usuarioId) {
        return agenteDelReporte != null && agenteDelReporte.equals(usuarioId);
    }

    // solo el ejecutivo de la empresa dueña del cobro reportado
    public static boolean ejecutivoPuede(UUID empresaDelReporte, UUID empresaDelEjecutivo) {
        return empresaDelReporte != null && empresaDelReporte.equals(empresaDelEjecutivo);
    }

    // sin agente asignado todavía no hay con quién conversar
    public static boolean tieneAgente(UUID agenteDelReporte) {
        return agenteDelReporte != null;
    }

    // resuelto: se puede leer, pero no escribir
    public static boolean soloLectura(String estadoReporte) {
        return "RESUELTO".equals(estadoReporte);
    }

    public static RolParticipante rol(boolean esAgente) {
        return esAgente ? RolParticipante.AGENTE : RolParticipante.EJECUTIVO;
    }

    // tamaño de página entre 1 y el máximo
    public static int limite(Integer pedido) {
        if (pedido == null || pedido < 1) return 30;
        return Math.min(pedido, LIMITE_MAXIMO);
    }
}
