package com.pagoflex.web.error;

import com.pagoflex.core.error.PagoFlexException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---------- manejo de excepciones ----------
    @ExceptionHandler(PagoFlexException.class)
    public ProblemDetail handlePagoFlexException(PagoFlexException ex) {
        // 1. construir la respuesta con el estado y mensaje del proyecto
        return problem(org.springframework.http.HttpStatusCode.valueOf(ex.getStatus()), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrorsMvc(MethodArgumentNotValidException ex) {
        // 1. agrupar los errores por campo
        return buildValidationProblemDetail(ex.getBindingResult().getFieldErrors());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es inválido o está mal formado");
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolationException(ConstraintViolationException ex) {
        // 1. asociar cada error con su parametro
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            errors.put(path.substring(path.lastIndexOf('.') + 1), violation.getMessage());
        });
        // 2. construir la respuesta de validacion
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Error en la validación de los parámetros");
        detail.setProperty("invalid_fields", errors);
        return detail;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handlePathVariableTypeMismatch(MethodArgumentTypeMismatchException ex) {
        // 1. conservar la excepcion del proyecto cuando corresponda
        if (ex.getCause() instanceof PagoFlexException pagoflex) {
            return handlePagoFlexException(pagoflex);
        }
        // 2. responder cuando el parametro tiene un tipo invalido
        return problem(HttpStatus.BAD_REQUEST, "Valor inválido para el parámetro '" + ex.getName() + "'");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        // 1. ocultar los detalles internos del error
        ProblemDetail detail = problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error inesperado. Por favor intenta de nuevo.");
        detail.setTitle("Error interno del servidor");
        return detail;
    }

    // --- helper privado ---
    private ProblemDetail buildValidationProblemDetail(List<FieldError> fieldErrors) {
        // 1. reunir los errores por campo
        ProblemDetail detail = problem(HttpStatus.BAD_REQUEST, "Error en la validación de los datos");
        Map<String, String> errors = new HashMap<>();
        fieldErrors.forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
        // 2. adjuntar los campos invalidos
        detail.setProperty("invalid_fields", errors);
        return detail;
    }

    // --- helper privado ---
    private ProblemDetail problem(org.springframework.http.HttpStatusCode status, String message) {
        // 1. construir el detalle del error
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
        detail.setTitle("Error en la solicitud");
        // 2. agregar la fecha de respuesta
        detail.setProperty("timestamp", Instant.now());
        return detail;
    }
}
