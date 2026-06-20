package com.Soham.urlshortener.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI configuration.
 *
 * Registers a "Bearer Authentication" security scheme so Swagger UI shows an
 * "Authorize" button where testers can paste their JWT token.
 * Once authorized, Swagger adds "Authorization: Bearer <token>" to all test requests.
 *
 * Access Swagger UI at: http://localhost:8080/swagger-ui/index.html
 * Raw OpenAPI JSON at:  http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
            .info(new Info()
                .title("URL Shortener API")
                .description("REST API for shortening URLs with click analytics")
                .version("1.0.0")
                .contact(new Contact().name("Soham Shah").email("sohamtshah2005@gmail.com")))
            // Apply JWT auth globally — all endpoints show the lock icon in Swagger UI
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Paste your JWT token from /api/auth/login")));
    }
}
