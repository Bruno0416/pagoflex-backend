package com.pagoflex.usuarios.features.auth.dto.request;

import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        String email,

        @NotBlank
        @StrongPassword
        String password,

        @NotBlank(message = "El rut es obligatorio")
        @Size(min = 9, max = 10, message = "El RUT debe tener 7 u 8 dígitos y un dígito verificador")
        @Pattern(regexp = "^\\d{7,8}-[0-9kK]$", message = "El RUT debe tener el formato 12345678-5, sin puntos")
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre no puede contener números ni caracteres especiales")
        String nombres,

        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido no puede contener números ni caracteres especiales")
        String apellidos
) {
}
