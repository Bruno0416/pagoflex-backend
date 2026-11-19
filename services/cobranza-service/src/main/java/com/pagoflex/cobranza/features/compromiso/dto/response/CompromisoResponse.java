package com.pagoflex.cobranza.features.compromiso.dto.response;

import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.entity.MotivoAnulacion;

import java.time.LocalDate;
import java.time.LocalDateTime;

/// Compromiso como lo ve el ejecutivo; de la persona solo nombre, rut y contacto RN-22
public record CompromisoResponse(
        String folio,
        String empresa,
        String concepto,
        Integer numeroCuota,
        Integer totalCuotas,
        long monto,
        long recargo,
        long total,
        LocalDate fechaVencimiento,
        EstadoCompromiso estado,
        long diasAtraso,
        PersonaResponse persona,
        String registradoPor,
        LocalDateTime fechaRegistro,
        AnulacionResponse anulacion,
        String numeroComprobante
) {
    public record PersonaResponse(String nombre, String rut, String correo, String telefono) {
    }

    public record AnulacionResponse(MotivoAnulacion motivo, String comentario, String ejecutivo,
                                    LocalDateTime fecha) {
    }
}
