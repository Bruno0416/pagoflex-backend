package com.pagoflex.soporte.features.reporte.dto.request;

import jakarta.validation.constraints.Size;

// el blanco lo reviso en el servicio para devolver el mensaje de RN-23
public record ResolverRequest(
        @Size(max = 1000, message = "La respuesta no puede superar 1000 caracteres")
        String respuesta
) {
}
