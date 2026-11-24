package com.pagoflex.cobranza.features.compromiso.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.service.CobranzaEjecutivoService;
import com.pagoflex.cobranza.features.empresa.dto.response.EmpresaResponse;
import com.pagoflex.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;
import java.util.List;

/// Cobranza del ejecutivo: siempre sobre la empresa del usuario autenticado
@RestController
@Tag(name = "Ejecutivo: cobranza de su empresa")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/empresas/me")
@RequiredArgsConstructor
public class EjecutivoCompromisoController {
    private final CobranzaEjecutivoService service;

    @Operation(summary = "Empresa del ejecutivo en sesión")
    @GetMapping
    public ResponseEntity<EmpresaResponse> empresa() {
        return ResponseEntity.ok(service.empresa(CurrentUser.id()));
    }

    @Operation(summary = "Resumen de cobranza del mes (tasa, recaudado, estados)")
    @GetMapping("/cobranza")
    public ResponseEntity<ResumenCobranzaResponse> resumen(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.resumen(CurrentUser.id(), mes));
    }

    // al registrar: si la persona ya existe no se piden sus datos RN-20
    @Operation(summary = "Buscar persona por RUT antes de registrar")
    @GetMapping("/personas/{rut}")
    public ResponseEntity<PersonaEncontradaResponse> persona(@PathVariable String rut) {
        return ResponseEntity.ok(service.buscarPersona(CurrentUser.id(), rut));
    }

    @Operation(summary = "Listar compromisos de la empresa por mes y estado")
    @GetMapping("/compromisos")
    public ResponseEntity<List<CompromisoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) EstadoCompromiso estado) {
        return ResponseEntity.ok(service.listar(CurrentUser.id(), mes, estado));
    }

    @Operation(summary = "Detalle de un compromiso de la empresa")
    @GetMapping("/compromisos/{folio}")
    public ResponseEntity<CompromisoResponse> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(service.detalle(CurrentUser.id(), folio));
    }

    @Operation(summary = "Registrar un compromiso (crea la persona si no existe)")
    @PostMapping("/compromisos")
    public ResponseEntity<CompromisoResponse> registrar(@Valid @RequestBody RegistrarCompromisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(CurrentUser.get(), request));
    }

    @Operation(summary = "Anular un compromiso con motivo")
    @PostMapping("/compromisos/{folio}/anulaciones")
    public ResponseEntity<CompromisoResponse> anular(@PathVariable String folio,
                                                     @Valid @RequestBody AnularCompromisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.anular(CurrentUser.get(), folio, request));
    }
}
