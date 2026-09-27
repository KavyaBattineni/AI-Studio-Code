package com.aivideo.platform.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes Swagger UI at /swagger-ui.html with a JWT "Authorize" button.
 * To call a protected endpoint from Swagger: POST /api/auth/login, copy the accessToken
 * from the response, click "Authorize" in the top-right of Swagger UI, and paste the raw
 * token (no "Bearer " prefix needed - Swagger adds it for you).
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI aiVideoStudioOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AI Video Studio API")
                        .description("REST API for the AI Video Studio SaaS platform: authentication, "
                                + "wallet, diamonds, payments, video generation, rewards and admin operations.")
                        .version("v1.0.0")
                        .contact(new Contact().name("AI Video Studio")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components().addSecuritySchemes(BEARER_SCHEME,
                        new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
