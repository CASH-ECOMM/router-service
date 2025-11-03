package com.cash.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI configuration for the API. Configures JWT security and automatic
 * global error responses
 * (400, 404, 500, 503).
 */
@Configuration
@OpenAPIDefinition(info = @Info(title = "Cash E-Commerce Router Service API", version = "1.0", description = "REST API Gateway for microservices including Catalogue, Auction, and User services"))
public class OpenApiConfig {

    /**
     * Configures the base OpenAPI specification. Includes JWT bearer token security
     * scheme.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .components(
                        new Components()
                                // JWT Security Scheme
                                .addSecuritySchemes(
                                        "bearer-jwt",
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer-jwt"));
    }

    /**
     * Customizes the OpenAPI specification to add global error responses. These
     * responses are
     * automatically added to all endpoints unless overridden. Also ensures
     * ErrorResponse schema is
     * registered.
     */
    @Bean
    public OpenApiCustomizer globalErrorResponseCustomizer() {
        return openApi -> {
            // Ensure ErrorResponse schema is in components
            if (openApi.getComponents() == null) {
                openApi.setComponents(new Components());
            }

            // Add ErrorResponse schema if not already present
            if (!openApi.getComponents().getSchemas().containsKey("ErrorResponse")) {
                Schema<?> errorResponseSchema = new Schema<>()
                        .type("object")
                        .description("Standard error response")
                        .addProperty(
                                "status",
                                new Schema<>().type("integer").description("HTTP status code").example(404))
                        .addProperty(
                                "message",
                                new Schema<>()
                                        .type("string")
                                        .description("Error message describing what went wrong")
                                        .example("Resource not found"));

                openApi.getComponents().addSchemas("ErrorResponse", errorResponseSchema);
            }

            // Reference to ErrorResponse schema
            Schema<?> errorResponseRef = new Schema<>().$ref("#/components/schemas/ErrorResponse");

            // Add global error responses that apply to all endpoints
            openApi
                    .getPaths()
                    .values()
                    .forEach(pathItem -> pathItem
                            .readOperations()
                            .forEach(
                                    operation -> {

                                        // 400 Bad Request - for validation errors
                                        if (!operation.getResponses().containsKey("400")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "400",
                                                            createErrorResponse(
                                                                    "Bad Request",
                                                                    "Invalid request parameters or validation error",
                                                                    errorResponseRef));
                                        }

                                        // 401 Unauthorized - for authentication errors
                                        if (!operation.getResponses().containsKey("401")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "401",
                                                            createErrorResponse(
                                                                    "Unauthorized",
                                                                    "Authentication required or invalid credentials",
                                                                    errorResponseRef));
                                        }

                                        // 404 Not Found - for resource not found errors
                                        if (!operation.getResponses().containsKey("404")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "404",
                                                            createErrorResponse(
                                                                    "Not Found",
                                                                    "Requested resource not found",
                                                                    errorResponseRef));
                                        }

                                        // 409 Conflict - for conflicting state errors
                                        if (!operation.getResponses().containsKey("409")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "409",
                                                            createErrorResponse(
                                                                    "Conflict",
                                                                    "Request conflicts with current state of the resource",
                                                                    errorResponseRef));
                                        }

                                        // 500 Internal Server Error - for unexpected errors
                                        if (!operation.getResponses().containsKey("500")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "500",
                                                            createErrorResponse(
                                                                    "Internal Server Error",
                                                                    "An unexpected error occurred",
                                                                    errorResponseRef));
                                        }

                                        // 503 Service Unavailable - for downstream service failures
                                        if (!operation.getResponses().containsKey("503")) {
                                            operation
                                                    .getResponses()
                                                    .addApiResponse(
                                                            "503",
                                                            createErrorResponse(
                                                                    "Service Unavailable",
                                                                    "Downstream service is temporarily unavailable",
                                                                    errorResponseRef));
                                        }
                                    }));
        };
    }

    /** Helper method to create an ApiResponse with error content. */
    private ApiResponse createErrorResponse(String description, String example, Schema<?> schema) {
        MediaType mediaType = new MediaType();
        mediaType.setSchema(schema);

        // Add example
        mediaType.example(
                String.format(
                        """
                                {
                                  "status": 500,
                                  "message": "%s"
                                }
                                """,
                        example));

        Content content = new Content();
        content.addMediaType("application/json", mediaType);

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setDescription(description);
        apiResponse.setContent(content);

        return apiResponse;
    }
}
