package com.pagoflex.cobranza.features.compromiso.service;

import com.pagoflex.cobranza.features.compromiso.dto.request.PagarRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.*;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

public interface CompromisoUsuarioService {
    List<MiCompromisoResponse> misCompromisos(UUID usuarioId, EstadoCompromiso estado, YearMonth mes);

    ResumenUsuarioResponse resumen(UUID usuarioId, YearMonth mes);

    MiCompromisoResponse detalle(UUID usuarioId, String folio);

    CotizacionResponse cotizar(UUID usuarioId, String folio);

    /// devuelve el pago y si es nuevo (false cuando se repite la misma llave)
    PagoConfirmado pagar(UUID usuarioId, String folio, String llave, PagarRequest request);

    List<PagoResponse> pagos(UUID usuarioId, String empresa, LocalDate desde, LocalDate hasta);

    ComprobanteResponse comprobante(UUID usuarioId, String numero);

    List<AnulacionUsuarioResponse> anulaciones(UUID usuarioId);

    CumplimientoResponse cumplimiento(UUID usuarioId);

    record PagoConfirmado(PagoResponse pago, boolean nuevo) {
    }
}
