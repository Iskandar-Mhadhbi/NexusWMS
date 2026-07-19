package com.nexuswms.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI/Swagger configuration for NexusWMS.
 *
 * <p>Global error responses are injected automatically into every endpoint
 * via {@link #globalResponseCustomizer()} — controllers only document their
 * specific success response. Runtime error handling is owned entirely by
 * {@link com.nexuswms.common.exception.GlobalExceptionHandler}.</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexusWMS API")
                        .description("Warehouse Operations & Fulfillment Platform")
                        .version("1.0.0"))
                .addServersItem(new Server().url("/"))
                .addSecurityItem(new SecurityRequirement().addList("BearerAuth"))
                .components(new Components()
                        .addSecuritySchemes("BearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by POST /api/v1/auth/login")));
    }

    /**
     * Injects standard error responses into every Swagger operation automatically.
     * Controllers remain clean — only their success response needs to be declared.
     * Does not override responses a controller has explicitly defined.
     */
    @Bean
    public OperationCustomizer globalResponseCustomizer() {
        return (operation, handlerMethod) -> {
            addIfAbsent(operation, "400", "Validation failed — request body or params are invalid");
            addIfAbsent(operation, "401", "Unauthorized — token missing, invalid, or revoked");
            addIfAbsent(operation, "403", "Forbidden — authenticated but insufficient role");
            addIfAbsent(operation, "404", "Resource not found");
            addIfAbsent(operation, "409", "Conflict — business rule or uniqueness violation");
            addIfAbsent(operation, "422", "Unprocessable — insufficient stock or constraint violation");
            addIfAbsent(operation, "500", "Internal server error");
            return operation;
        };
    }

    private void addIfAbsent(
            io.swagger.v3.oas.models.Operation operation,
            String statusCode,
            String description
    ) {
        if (!operation.getResponses().containsKey(statusCode)) {
            operation.getResponses().addApiResponse(
                    statusCode, new ApiResponse().description(description));
        }
    }
}