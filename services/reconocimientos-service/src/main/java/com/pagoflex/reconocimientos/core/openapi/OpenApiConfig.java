package com.pagoflex.reconocimientos.core.openapi;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "PagoFlex - Reconocimientos",
        version = "1.0.0",
        description = "API REST de reconocimientos"))
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
