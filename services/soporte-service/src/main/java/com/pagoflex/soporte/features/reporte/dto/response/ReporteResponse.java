package com.pagoflex.soporte.features.reporte.dto.response;

import com.pagoflex.soporte.features.reporte.entity.EstadoReporte;
import com.pagoflex.soporte.features.reporte.entity.MotivoReporte;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReporteResponse(
        String numero,
        String folio,
        MotivoReporte motivo,
        String comentario,
        EstadoReporte estado,
        boolean atrasado,
        LocalDateTime fechaCreacion,
        CobroResponse cobro,
        UsuarioResponse usuario,
        String agente,
        LocalDateTime fechaToma,
        String respuesta,
        LocalDateTime fechaResolucion
) {
    public record CobroResponse(String empresa, String concepto, Integer numeroCuota, Integer totalCuotas,
                                long total, LocalDate fechaVencimiento, String estado) {
    }

    public record UsuarioResponse(String nombre, String correo, String telefono) {
    }
}
