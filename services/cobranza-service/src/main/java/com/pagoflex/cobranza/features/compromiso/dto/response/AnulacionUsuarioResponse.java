package com.pagoflex.cobranza.features.compromiso.dto.response;

import com.pagoflex.cobranza.features.compromiso.entity.MotivoAnulacion;

import java.time.LocalDateTime;

public record AnulacionUsuarioResponse(
        String folio,
        String empresa,
        String concepto,
        long monto,
        MotivoAnulacion motivo,
        String comentario,
        LocalDateTime fecha
) {
}
