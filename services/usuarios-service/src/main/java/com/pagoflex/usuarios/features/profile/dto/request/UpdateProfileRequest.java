package com.pagoflex.usuarios.features.profile.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Pattern(regexp = "^[\\p{L}][\\p{L} '-]{0,79}$", message = "El nombre contiene caracteres no permitidos")
        String nombres,
        @Pattern(regexp = "^[\\p{L}][\\p{L} '-]{0,79}$", message = "El apellido contiene caracteres no permitidos")
        String apellidos,
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String correo,
        String contrasenaActual
) {}
