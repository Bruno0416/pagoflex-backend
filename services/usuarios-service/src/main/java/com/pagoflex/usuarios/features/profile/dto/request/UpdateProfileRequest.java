package com.pagoflex.usuarios.features.profile.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(example = """
        {
          "nombres": "Camila Andrea",
          "apellidos": "Soto Pérez"
        }
        """)
public record UpdateProfileRequest(
        @Pattern(regexp = "^[\\p{L}][\\p{L} '-]{0,79}$", message = "El nombre contiene caracteres no permitidos")
        @Schema(description = "Nombres actualizados; omitir si no se modifican", example = "Camila Andrea")
        String nombres,
        @Pattern(regexp = "^[\\p{L}][\\p{L} '-]{0,79}$", message = "El apellido contiene caracteres no permitidos")
        @Schema(description = "Apellidos actualizados; omitir si no se modifican", example = "Soto Pérez")
        String apellidos,
        @Email(message = "El correo no es válido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        @Schema(description = "Nuevo correo; omitir si no se modifica", example = "camila.nuevo@example.com")
        String correo,
        @Schema(description = "Contraseña vigente; obligatoria solo cuando cambia el correo", example = "Demo123!")
        String contrasenaActual
) {}
