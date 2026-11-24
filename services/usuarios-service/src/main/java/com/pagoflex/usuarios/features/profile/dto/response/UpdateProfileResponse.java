package com.pagoflex.usuarios.features.profile.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import com.pagoflex.usuarios.features.auth.dto.response.AuthResponse;

@Schema(example = """
        {
          "perfil": {
            "id": "d8a6b770-cd91-4fe9-982b-7f066da55ae1",
            "idPersona": "2f61d92a-0f35-4a20-9e69-b306bded503a",
            "rut": "12345678-5",
            "nombres": "Camila Andrea",
            "apellidos": "Soto Pérez",
            "correo": "camila.ejemplo@example.com",
            "telefono": "+56912345678",
            "rol": "USUARIO_FINAL",
            "urlAvatar": "/api/v1/usuarios/me/avatar?v=03a23527-e203-4dce-a49c-9acaf41c0617.jpg",
            "idEmpresa": null
          },
          "sesion": null
        }
        """)
public record UpdateProfileResponse(
        @Schema(description = "Perfil actualizado")
        ProfileResponse perfil,
        @Schema(description = "Nuevas credenciales solo si cambió el correo; null en los demás casos")
        AuthResponse sesion
) {}
