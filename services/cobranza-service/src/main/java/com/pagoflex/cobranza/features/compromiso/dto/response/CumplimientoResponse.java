package com.pagoflex.cobranza.features.compromiso.dto.response;

import java.time.LocalDate;

/// porcentaje nulo cuando hay menos de tres compromisos elegibles RN-24
public record CumplimientoResponse(
        LocalDate desde,
        LocalDate hasta,
        int elegibles,
        int pagadosATiempo,
        Double porcentaje,
        String categoria
) {
}
