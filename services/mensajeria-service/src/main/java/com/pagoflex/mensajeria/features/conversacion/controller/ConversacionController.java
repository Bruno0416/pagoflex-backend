package com.pagoflex.mensajeria.features.conversacion.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.pagoflex.mensajeria.features.conversacion.dto.request.EnviarMensajeRequest;
import com.pagoflex.mensajeria.features.conversacion.dto.request.LecturaRequest;
import com.pagoflex.mensajeria.features.conversacion.dto.response.ConversacionResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.MensajeResponse;
import com.pagoflex.mensajeria.features.conversacion.dto.response.PaginaMensajesResponse;
import com.pagoflex.mensajeria.features.conversacion.service.ConversacionService;
import com.pagoflex.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/// Chat interno del reporte entre el agente asignado y el ejecutivo de la empresa
@RestController
@Tag(name = "Agente y ejecutivo: chat del reporte")
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/v1/conversaciones/reportes/{numero}")
@RequiredArgsConstructor
public class ConversacionController {
    private final ConversacionService service;

    @Operation(summary = "Abrir la conversación del reporte")
    @GetMapping
    public ResponseEntity<ConversacionResponse> abrir(@PathVariable String numero) {
        return ResponseEntity.ok(service.abrir(CurrentUser.get(), numero));
    }

    @Operation(summary = "Historial de mensajes por cursor")
    @GetMapping("/mensajes")
    public ResponseEntity<PaginaMensajesResponse> mensajes(@PathVariable String numero,
                                                           @RequestParam(required = false) Long cursor,
                                                           @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(service.mensajes(CurrentUser.get(), numero, cursor, limit));
    }

    // reintentar con el mismo clientMessageId devuelve el mismo mensaje con 200
    @Operation(summary = "Enviar un mensaje (idempotente con clientMessageId)")
    @PostMapping("/mensajes")
    public ResponseEntity<MensajeResponse> enviar(@PathVariable String numero,
                                                  @Valid @RequestBody EnviarMensajeRequest request) {
        ConversacionService.Enviado r = service.enviar(CurrentUser.get(), numero, request);
        return ResponseEntity.status(r.nuevo() ? HttpStatus.CREATED : HttpStatus.OK).body(r.mensaje());
    }

    // mensajes en vivo; al reconectar la app manda el último id que recibió
    @Operation(summary = "Mensajes en vivo por SSE (reconecta con Last-Event-ID)")
    @GetMapping(value = "/eventos", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter eventos(@PathVariable String numero,
                              @RequestHeader(value = "Last-Event-ID", required = false) Long ultimoVisto) {
        return service.eventos(CurrentUser.get(), numero, ultimoVisto);
    }

    @Operation(summary = "Marcar mensajes como leídos")
    @PutMapping("/lecturas")
    public ResponseEntity<Void> leer(@PathVariable String numero, @Valid @RequestBody LecturaRequest request) {
        service.marcarLeido(CurrentUser.get(), numero, request.ultimaSecuencia());
        return ResponseEntity.noContent().build();
    }
}
