package com.pagoflex.cobranza.features.compromiso.controller;

import com.pagoflex.cobranza.features.compromiso.service.HechosPagoService;
import com.pagoflex.contracts.cobranza.PaymentFactsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/// Reconocimientos-service pide aquí el historial de pagos de un usuario
@RestController
@RequestMapping("/api/v1/internal/usuarios")
@RequiredArgsConstructor
public class HechosPagoInternoController {
    private final HechosPagoService service;

    @GetMapping("/{idUsuario}/hechos-pago")
    public PaymentFactsResponse hechosPago(@PathVariable UUID idUsuario) {
        return service.deUsuario(idUsuario);
    }
}
