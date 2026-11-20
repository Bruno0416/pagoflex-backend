package com.pagoflex.cobranza.features.compromiso.dto.response;

import java.time.YearMonth;

public record ResumenCobranzaResponse(
        String empresa,
        YearMonth mes,
        int totalCompromisos,
        int pagados,
        int pendientes,
        int vencidos,
        int enRevision,
        int anulados,
        double tasaCobranza,
        long recaudado
) {
}
