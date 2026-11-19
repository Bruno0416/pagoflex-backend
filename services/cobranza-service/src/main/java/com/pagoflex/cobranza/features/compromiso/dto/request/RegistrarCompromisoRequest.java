package com.pagoflex.cobranza.features.compromiso.dto.request;

import jakarta.validation.constraints.*;

import java.time.LocalDate;

/// Los datos de la persona solo son obligatorios si el rut aún no está registrado RN-20
public record RegistrarCompromisoRequest(
        @NotBlank(message = "El rut es obligatorio")
        String rut,

        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El nombre no puede contener números ni caracteres especiales")
        String nombres,

        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El apellido no puede contener números ni caracteres especiales")
        String apellidos,

        @Email(message = "Debe ser un correo válido: email@example.com")
        String correo,

        @Pattern(regexp = "^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "El teléfono debe tener el formato +56 9 1234 5678")
        String telefono,

        @NotBlank(message = "El concepto es obligatorio")
        @Size(max = 255, message = "El concepto no puede superar los 255 caracteres")
        String concepto,

        @NotNull(message = "El monto es obligatorio")
        @Positive(message = "El monto debe ser mayor que cero")
        Long monto,

        @NotNull(message = "La fecha de vencimiento es obligatoria")
        LocalDate fechaVencimiento,

        @Positive(message = "La cuota debe ser mayor que cero")
        Integer numeroCuota,

        @Positive(message = "El total de cuotas debe ser mayor que cero")
        Integer totalCuotas
) {
}
