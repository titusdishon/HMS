package com.devdishon.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name:Health Management System}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .version("1.0.0")
                        .description("""
                                REST API for Health Management System.

                                ## Authentication

                                This API uses JWT (JSON Web Token) for authentication.

                                To authenticate:
                                1. Register a new user via `/api/v1/auth/register`
                                2. Login via `/api/v1/auth/login` to receive an access token
                                3. Include the token in the `Authorization` header as `Bearer <token>`

                                ## Roles

                                - **USER**: Read access to most resources
                                - **ADMIN**: Read and write access
                                - **SUPER_ADMIN**: Full access including delete operations
                                """)
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com")
                                .url("https://github.com/your-org/health-management-system"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("/").description("Current Server")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter your JWT token obtained from /api/v1/auth/login")));
    }
}
