package com.usarbcs.wallet.service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springdoc.core.customizers.OpenApiCustomizer;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiConfigTest {

    private final OpenApiConfig config = new OpenApiConfig();

    @Test
    void walletServiceOpenApiDefinesInfoAndServer() {
        OpenAPI openAPI = config.walletServiceOpenAPI("wallet-service", "/wallet");

        assertThat(openAPI.getInfo()).isNotNull();
        assertThat(openAPI.getInfo().getTitle()).isEqualTo("wallet-service API");
        assertThat(openAPI.getInfo().getDescription()).contains("wallet microservice");
        assertThat(openAPI.getServers()).hasSize(1);
        assertThat(openAPI.getServers().get(0).getUrl()).isEqualTo("/wallet");
    }

    @Test
    void rfc7807CustomizerRegistersSchemasAndResponses() {
        OpenAPI openAPI = new OpenAPI();
        Components components = new Components();
        openAPI.setComponents(components);

        OpenApiCustomizer customizer = config.rfc7807ComponentsCustomiser();
        customizer.customise(openAPI);

        assertThat(components.getSchemas()).containsKeys("ProblemDetail", "ValidationProblemDetail");

        ApiResponse validationProblem = components.getResponses().get("ValidationProblem");
        Schema<?> validationSchema = validationProblem.getContent().get("application/problem+json").getSchema();
        assertThat(validationSchema.get$ref()).endsWith("/ValidationProblemDetail");

        ApiResponse businessProblem = components.getResponses().get("BusinessProblem");
        assertThat(businessProblem.getContent().get("application/problem+json")
                .getSchema().get$ref()).endsWith("/ProblemDetail");

        ApiResponse internalProblem = components.getResponses().get("InternalProblem");
        assertThat(internalProblem.getContent().get("application/problem+json").getExamples())
                .containsKey("Unexpected internal error");
    }
}
