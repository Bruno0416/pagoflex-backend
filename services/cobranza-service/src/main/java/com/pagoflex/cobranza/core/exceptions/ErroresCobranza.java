package com.pagoflex.cobranza.core.exceptions;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/// Agrega code y details al formato de error común; el resto lo maneja libs/web
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErroresCobranza {

    @ExceptionHandler(ReglaRechazada.class)
    public ProblemDetail regla(ReglaRechazada ex) {
        // 1. mismo formato que el resto, con el código estable
        ProblemDetail detail = problema(HttpStatusCode.valueOf(ex.getStatus()), ex.getMessage(), ex.getCode());
        if (!ex.getDetails().isEmpty()) detail.setProperty("details", ex.getDetails());
        return detail;
    }

    // dos operaciones al mismo tiempo sobre el mismo compromiso
    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, DataIntegrityViolationException.class})
    public ProblemDetail concurrencia(RuntimeException ex) {
        return problema(HttpStatus.CONFLICT, "El compromiso cambió mientras se procesaba, vuelve a intentarlo",
                "CONCURRENT_UPDATE");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail cabecera(MissingRequestHeaderException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Falta la cabecera " + ex.getHeaderName(), "MISSING_HEADER");
    }

    // --- helper privado ---
    private ProblemDetail problema(HttpStatusCode status, String mensaje, String code) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, mensaje);
        detail.setTitle("Error en la solicitud");
        detail.setProperty("timestamp", Instant.now());
        detail.setProperty("code", code);
        return detail;
    }
}
