package com.pagoflex.cobranza.features.compromiso.dto.response;

import java.time.YearMonth;
import java.util.List;

/// tarjeta de inicio: lo que queda del mes, lo pagado y lo atrasado
public record ResumenUsuarioResponse(
        YearMonth mes,
        long porPagarMes,
        long pagadoMes,
        int pendientesMes,
        int vencidos,
        long montoVencido,
        List<MiCompromisoResponse> proximos
) {
}
