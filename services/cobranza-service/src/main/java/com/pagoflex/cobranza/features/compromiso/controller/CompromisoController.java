package com.pagoflex.cobranza.features.compromiso.controller;

import com.pagoflex.cobranza.features.compromiso.dto.request.AnularCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.request.RegistrarCompromisoRequest;
import com.pagoflex.cobranza.features.compromiso.dto.response.CompromisoResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.PersonaEncontradaResponse;
import com.pagoflex.cobranza.features.compromiso.dto.response.ResumenCobranzaResponse;
import com.pagoflex.cobranza.features.compromiso.entity.EstadoCompromiso;
import com.pagoflex.cobranza.features.compromiso.service.CompromisoService;
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
@RequestMapping("/api/v1/cobranza")
@RequiredArgsConstructor
public class CompromisoController {
    private final CompromisoService service;

    @GetMapping("/empresa")
    public ResponseEntity<EmpresaResponse> empresa() {
        return ResponseEntity.ok(service.empresa(CurrentUser.get().email()));
    }

    // al registrar: si la persona ya existe no se piden sus datos RN-20
    @GetMapping("/personas/{rut}")
    public ResponseEntity<PersonaEncontradaResponse> persona(@PathVariable String rut) {
        return ResponseEntity.ok(service.buscarPersona(CurrentUser.get().email(), rut));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenCobranzaResponse> resumen(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        return ResponseEntity.ok(service.resumen(CurrentUser.get().email(), mes));
    }

    @GetMapping("/compromisos")
    public ResponseEntity<List<CompromisoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) EstadoCompromiso estado) {
        return ResponseEntity.ok(service.listar(CurrentUser.get().email(), mes, estado));
    }

    // usuario final: sus compromisos de todas las empresas
    @GetMapping("/mis-compromisos")
    public ResponseEntity<List<CompromisoResponse>> misCompromisos() {
        return ResponseEntity.ok(service.deUsuario(CurrentUser.get().email()));
    }

    @GetMapping("/compromisos/{folio}")
    public ResponseEntity<CompromisoResponse> detalle(@PathVariable String folio) {
        return ResponseEntity.ok(service.detalle(CurrentUser.get().email(), folio));
    }

    @PostMapping("/compromisos")
    public ResponseEntity<CompromisoResponse> registrar(@Valid @RequestBody RegistrarCompromisoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(CurrentUser.get().email(), request));
    }

    @PostMapping("/compromisos/{folio}/anulacion")
    public ResponseEntity<CompromisoResponse> anular(@PathVariable String folio,
                                                     @Valid @RequestBody AnularCompromisoRequest request) {
        return ResponseEntity.ok(service.anular(CurrentUser.get().email(), folio, request));
    }
}
