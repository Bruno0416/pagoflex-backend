package com.pagoflex.mensajeria.core.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

// documentación swagger: /swagger-ui.html y /v3/api-docs
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "PagoFlex - Mensajería",
        version = "1.0.0",
        description = "Chat interno de cada reporte entre el agente asignado y el ejecutivo de la empresa"))
@SecuritySchemes({
        @SecurityScheme(
                name = "bearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT"),
        @SecurityScheme(
                name = "internalApiKey",
                type = SecuritySchemeType.APIKEY,
                in = SecuritySchemeIn.HEADER,
                paramName = "X-Internal-Api-Key")
})
public class OpenApiConfig {}
