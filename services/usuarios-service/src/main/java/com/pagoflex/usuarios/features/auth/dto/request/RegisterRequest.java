package com.pagoflex.usuarios.features.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pagoflex.contracts.organizacion.SolicitudValidacionCodigoEmpresa;
import com.pagoflex.usuarios.core.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(example = """
        {
          "correo": "camila.ejemplo@example.com",
          "contrasena": "Demo123!",
          "rut": "12345678-5",
          "nombres": "Camila",
          "apellidos": "Soto Pérez",
          "codigoEmpresa": null
        }
        """)
public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio")
        @Email(message = "Debe ser un correo válido: email@example.com")
        @Schema(description = "Correo de la nueva cuenta", example = "camila.ejemplo@example.com")
        String correo,

        @NotBlank
        @StrongPassword
        @Schema(description = "Mínimo 8 caracteres, con mayúscula, minúscula, número y carácter especial", example = "Demo123!")
        String contrasena,

        @NotBlank(message = "El rut es obligatorio")
        @Size(min = 9, max = 10, message = "El RUT debe tener 7 u 8 dígitos y un dígito verificador")
        @Pattern(regexp = "^\\d{7,8}-[0-9kK]$", message = "El RUT debe tener el formato 12345678-5, sin puntos")
        @Schema(description = "RUT chileno sin puntos, con guion y dígito verificador", example = "12345678-5")
        String rut,

        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre no puede contener números ni caracteres especiales")
        @Schema(description = "Nombres de la persona", example = "Camila")
        String nombres,

        @NotBlank(message = "El nombre es obligatorio")
        @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido no puede contener números ni caracteres especiales")
        @Schema(description = "Apellidos de la persona", example = "Soto Pérez")
        String apellidos,

        @Pattern(regexp = SolicitudValidacionCodigoEmpresa.FORMATO,
                message = "El codigo de empresa no tiene un formato valido")
        @Schema(description = "Opcional. Omitir o enviar null para usuario final; para agente o ejecutivo usar el código de seis caracteres de la organización (AA00AA)", example = "AB12CD")
        String codigoEmpresa
) {
}
