package com.pagoflex.usuarios.features.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(example = """
        {
          "id": "d8a6b770-cd91-4fe9-982b-7f066da55ae1",
          "idPersona": "2f61d92a-0f35-4a20-9e69-b306bded503a",
          "rut": "12345678-5",
          "nombres": "Camila",
          "apellidos": "Soto Pérez",
          "correo": "camila.ejemplo@example.com",
          "telefono": "+56912345678",
          "rol": "USUARIO_FINAL",
          "urlAvatar": "/api/v1/usuarios/me/avatar?v=03a23527-e203-4dce-a49c-9acaf41c0617.jpg",
          "idEmpresa": null
        }
        """)
public record ProfileResponse(
        @Schema(description = "Identificador de la cuenta", example = "d8a6b770-cd91-4fe9-982b-7f066da55ae1")
        UUID id,
        @Schema(description = "Identificador de la persona vinculada", example = "2f61d92a-0f35-4a20-9e69-b306bded503a")
        UUID idPersona,
        @Schema(description = "RUT sin puntos y con guion", example = "12345678-5")
        String rut,
        @Schema(description = "Nombres de la persona", example = "Camila")
        String nombres,
        @Schema(description = "Apellidos de la persona", example = "Soto Pérez")
        String apellidos,
        @Schema(description = "Correo de la cuenta", example = "camila.ejemplo@example.com")
        String correo,
        @Schema(description = "Teléfono de contacto, si está registrado", example = "+56912345678")
        String telefono,
        @Schema(description = "Rol de la cuenta", example = "USUARIO_FINAL")
        String rol,
        @Schema(description = "Ruta relativa del avatar a través del gateway, o null si no tiene avatar", example = "/api/v1/usuarios/me/avatar?v=03a23527-e203-4dce-a49c-9acaf41c0617.jpg")
        String urlAvatar,
        @Schema(description = "Identificador de la organización; null para usuario final", example = "ac581dc9-bcd2-4657-b29f-678481594cd1")
        UUID idEmpresa
) {}
