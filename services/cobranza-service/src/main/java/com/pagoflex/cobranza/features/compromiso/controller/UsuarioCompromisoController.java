package com.pagoflex.cobranza.features.compromiso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.core.client.ReconocimientosClient;
import com.pagoflex.cobranza.features.compromiso.dto.request.PagarRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.*;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.service.CompromisoUsuarioService;
import com.pagoflex.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/// Usuario final: solo sus propios compromisos, pagos y comprobantes
@RestController
@Tag(name = "Usuario final: compromisos y pagos")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsuarioCompromisoController {
    private final CompromisoUsuarioService service;
    private final ReconocimientosClient reconocimientos;

    @Operation(summary = "Mis compromisos (filtro por estado y mes)")
    @GetMapping("/compromisos/me")
    public ResponseEntity<List<MiCompromisoResponse>> misCompromisos(
            @RequestParam(required = false) EstadoCompromiso estado,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.misCompromisos(CurrentUser.id(), estado, mes));
    }

    @Operation(summary = "Resumen del mes: por pagar, pagado y vencidos")
    @GetMapping("/compromisos/me/resumen")
    public ResponseEntity<ResumenUsuarioResponse> resumen(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.resumen(CurrentUser.id(), mes));
    }

    @Operation(summary = "Detalle de un compromiso propio")
    @GetMapping("/compromisos/me/{folio}")
    public ResponseEntity<MiCompromisoResponse> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(service.detalle(CurrentUser.id(), folio));
    }

    @Operation(summary = "Cotizar el pago (total con recargo y cotizacionId)")
    @GetMapping("/compromisos/me/{folio}/cotizacion-pago")
    public ResponseEntity<CotizacionResponse> cotizar(@PathVariable String folio) {
        return ResponseEntity.ok(service.cotizar(CurrentUser.id(), folio));
    }

    // reintentar con la misma Idempotency-Key devuelve el mismo pago con 200
    @Operation(summary = "Pagar un compromiso (pago simulado con Idempotency-Key)")
    @PostMapping("/compromisos/me/{folio}/pagos")
    public ResponseEntity<PagoResponse> pagar(@PathVariable String folio,
                                              @RequestHeader("Idempotency-Key") String llave,
                                              @Valid @RequestBody PagarRequest request) {
        CompromisoUsuarioService.PagoConfirmado r = service.pagar(CurrentUser.id(), folio, llave, request);
        // el pago ya quedó confirmado; si reconocimientos falla no se revierte, y repetir el pago con la misma llave reintenta
        reconocimientos.evaluar(CurrentUser.id(), null);
        return ResponseEntity.status(r.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(r.pago());
    }

    @Operation(summary = "Historial de pagos")
    @GetMapping("/pagos/me")
    public ResponseEntity<List<PagoResponse>> pagos(
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.pagos(CurrentUser.id(), empresa, desde, hasta));
    }

    @Operation(summary = "Comprobante de un pago")
    @GetMapping("/comprobantes/{numero}")
    public ResponseEntity<ComprobanteResponse> comprobante(@PathVariable String numero) {
        return ResponseEntity.ok(service.comprobante(CurrentUser.id(), numero));
    }

    @Operation(summary = "Compromisos anulados")
    @GetMapping("/anulaciones/me")
    public ResponseEntity<List<AnulacionUsuarioResponse>> anulaciones() {
        return ResponseEntity.ok(service.anulaciones(CurrentUser.id()));
    }

    @Operation(summary = "Porcentaje y categoría de cumplimiento")
    @GetMapping("/cumplimiento/me")
    public ResponseEntity<CumplimientoResponse> cumplimiento() {
        return ResponseEntity.ok(service.cumplimiento(CurrentUser.id()));
    }
}
