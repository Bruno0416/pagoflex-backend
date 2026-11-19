package com.pagoflex.cobranza.features.compromiso.controller;

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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UsuarioCompromisoController {
    private final CompromisoUsuarioService service;

    @GetMapping("/compromisos/me")
    public ResponseEntity<List<MiCompromisoResponse>> misCompromisos(
            @RequestParam(required = false) EstadoCompromiso estado,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.misCompromisos(CurrentUser.id(), estado, mes));
    }

    @GetMapping("/compromisos/me/resumen")
    public ResponseEntity<ResumenUsuarioResponse> resumen(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.resumen(CurrentUser.id(), mes));
    }

    @GetMapping("/compromisos/me/{folio}")
    public ResponseEntity<MiCompromisoResponse> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(service.detalle(CurrentUser.id(), folio));
    }

    @GetMapping("/compromisos/me/{folio}/cotizacion-pago")
    public ResponseEntity<CotizacionResponse> cotizar(@PathVariable String folio) {
        return ResponseEntity.ok(service.cotizar(CurrentUser.id(), folio));
    }

    // reintentar con la misma Idempotency-Key devuelve el mismo pago con 200
    @PostMapping("/compromisos/me/{folio}/pagos")
    public ResponseEntity<PagoResponse> pagar(@PathVariable String folio,
                                              @RequestHeader("Idempotency-Key") String llave,
                                              @Valid @RequestBody PagarRequest request) {
        CompromisoUsuarioService.PagoConfirmado r = service.pagar(CurrentUser.id(), folio, llave, request);
        return ResponseEntity.status(r.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(r.pago());
    }

    @GetMapping("/pagos/me")
    public ResponseEntity<List<PagoResponse>> pagos(
            @RequestParam(required = false) String empresa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(service.pagos(CurrentUser.id(), empresa, desde, hasta));
    }

    @GetMapping("/comprobantes/{numero}")
    public ResponseEntity<ComprobanteResponse> comprobante(@PathVariable String numero) {
        return ResponseEntity.ok(service.comprobante(CurrentUser.id(), numero));
    }

    @GetMapping("/anulaciones/me")
    public ResponseEntity<List<AnulacionUsuarioResponse>> anulaciones() {
        return ResponseEntity.ok(service.anulaciones(CurrentUser.id()));
    }

    @GetMapping("/cumplimiento/me")
    public ResponseEntity<CumplimientoResponse> cumplimiento() {
        return ResponseEntity.ok(service.cumplimiento(CurrentUser.id()));
    }
}
