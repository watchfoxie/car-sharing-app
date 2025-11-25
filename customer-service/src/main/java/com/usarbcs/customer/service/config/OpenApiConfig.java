package com.usarbcs.customer.service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralizes the OpenAPI metadata so the swagger-ui and /v3/api-docs endpoints
 * expose consistent information regardless of the context-path configured per environment.
 */
@Configuration
public class OpenApiConfig {

        private static final String MEDIA_TYPE_PROBLEM_JSON = "application/problem+json";
        private static final String PROBLEM_SCHEMA = "ProblemDetail";
        private static final String VALIDATION_PROBLEM_SCHEMA = "ValidationProblemDetail";
        private static final String PROBLEM_BASE_URI = "https://car-sharing.app/problems";
        private static final String VALIDATION_TYPE_URI = PROBLEM_BASE_URI + "/request-validation";
        private static final String BUSINESS_TYPE_URI = PROBLEM_BASE_URI + "/business-rule-violation";
        private static final String INTERNAL_TYPE_URI = PROBLEM_BASE_URI + "/internal-error";
        private static final String TITLE_VALIDATION = "Request validation failed";
        private static final String TITLE_BUSINESS = "Business rule violation";
        private static final String TITLE_INTERNAL = "Unexpected internal error";
        private static final String INSTANCE_EXAMPLE = "/api/v1/customers";
        private static final String PROP_TYPE = "type";
        private static final String PROP_TITLE = "title";
        private static final String PROP_STATUS = "status";
        private static final String PROP_DETAIL = "detail";
        private static final String PROP_INSTANCE = "instance";
        private static final String PROP_ERRORS = "errors";
        private static final String PROP_ERROR_CODE = "errorCode";
        private static final String PROP_REFERENCE = "reference";

        @Bean
        public OpenAPI customerServiceOpenAPI(
                        @Value("${spring.application.name:customer-service}") String applicationName,
                        @Value("${server.servlet.context-path:}") String contextPath) {
                final String normalizedContextPath = StringUtils.hasText(contextPath) ? contextPath : "/";
                return new OpenAPI()
                                .info(new Info()
                                                .title(applicationName + " API")
                                                .description("OpenAPI contract for the customer microservice.")
                                                .version("v1"))
                                .servers(List.of(new Server().url(normalizedContextPath).description("Base path")));
        }

        @Bean
        public OpenApiCustomizer rfc7807ComponentsCustomiser() {
                return openApi -> {
                        Components components = openApi.getComponents();
                        if (components == null) {
                                components = new Components();
                                openApi.setComponents(components);
                        }
                        registerProblemSchemas(components);
                        registerProblemResponses(components);
                };
        }

        private void registerProblemSchemas(Components components) {
                if (!hasSchema(components, PROBLEM_SCHEMA)) {
                        components.addSchemas(PROBLEM_SCHEMA, baseProblemSchema());
                }
                if (!hasSchema(components, VALIDATION_PROBLEM_SCHEMA)) {
                        components.addSchemas(VALIDATION_PROBLEM_SCHEMA, validationProblemSchema());
                }
        }

        private void registerProblemResponses(Components components) {
                if (!hasResponse(components, "ValidationProblem")) {
                        components.addResponses("ValidationProblem",
                                        problemResponse(TITLE_VALIDATION, VALIDATION_PROBLEM_SCHEMA, validationExample()));
                }
                if (!hasResponse(components, "BusinessProblem")) {
                        components.addResponses("BusinessProblem",
                                        problemResponse(TITLE_BUSINESS, PROBLEM_SCHEMA, businessExample()));
                }
                if (!hasResponse(components, "InternalProblem")) {
                        components.addResponses("InternalProblem",
                                        problemResponse(TITLE_INTERNAL, PROBLEM_SCHEMA, internalServerExample()));
                }
        }

        private Schema<?> baseProblemSchema() {
                ObjectSchema schema = new ObjectSchema();
                schema.addProperty(PROP_TYPE, new StringSchema().format("uri").description("Problem category URI."));
                schema.addProperty(PROP_TITLE, new StringSchema().description("Short, human readable summary of the problem."));
                schema.addProperty(PROP_STATUS, new Schema<>().type("integer").format("int32").description("HTTP status code."));
                schema.addProperty(PROP_DETAIL, new StringSchema().description("Detailed, contextual explanation."));
                schema.addProperty(PROP_INSTANCE, new StringSchema().format("uri").description("URI of the failing request."));
                schema.addProperty(PROP_ERROR_CODE, new Schema<>().type("integer").format("int32").description("Domain specific error code.").nullable(true));
                schema.addProperty(PROP_REFERENCE, new StringSchema().description("Trace identifier to help troubleshooting.").nullable(true));
                schema.setRequired(List.of(PROP_TYPE, PROP_TITLE, PROP_STATUS, PROP_DETAIL, PROP_INSTANCE));
                return schema;
        }

        private Schema<?> validationProblemSchema() {
                Schema<?> errorsSchema = new Schema<>()
                                .type("object")
                                .description("Map of field names to the list of validation messages.")
                                .additionalProperties(new ArraySchema().items(new StringSchema().example("must not be blank")));

                return new ObjectSchema()
                                .allOf(List.of(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA)))
                                .addProperty(PROP_ERRORS, errorsSchema);
        }

        private ApiResponse problemResponse(String description, String schemaName, Example example) {
                MediaType mediaType = new MediaType()
                                .schema(new Schema<>().$ref("#/components/schemas/" + schemaName));
                if (example != null) {
                        mediaType.addExamples(example.getSummary(), example);
                }
                return new ApiResponse()
                                .description(description)
                                .content(new Content().addMediaType(MEDIA_TYPE_PROBLEM_JSON, mediaType));
        }

        private Example validationExample() {
                Example example = new Example();
                example.setSummary("Validation error");
                example.setDescription("RFC 7807 representation for payload validation failures.");
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put(PROP_TYPE, VALIDATION_TYPE_URI);
                payload.put(PROP_TITLE, TITLE_VALIDATION);
                payload.put(PROP_STATUS, 400);
                payload.put(PROP_DETAIL, "Payload validation failed. See the errors property for details.");
                payload.put(PROP_INSTANCE, INSTANCE_EXAMPLE);
                payload.put(PROP_ERRORS, Map.of(
                                "email", List.of("must be a well-formed email address"),
                                "password", List.of("size must be between 8 and 64")
                ));
                example.setValue(payload);
                return example;
        }

        private Example businessExample() {
                Example example = new Example();
                example.setSummary(TITLE_BUSINESS);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put(PROP_TYPE, BUSINESS_TYPE_URI);
                payload.put(PROP_TITLE, TITLE_BUSINESS);
                payload.put(PROP_STATUS, 409);
                payload.put(PROP_DETAIL, "Customer workflow rule violated.");
                payload.put(PROP_INSTANCE, INSTANCE_EXAMPLE);
                payload.put(PROP_ERROR_CODE, 16);
                example.setValue(payload);
                return example;
        }

        private Example internalServerExample() {
                Example example = new Example();
                example.setSummary(TITLE_INTERNAL);
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put(PROP_TYPE, INTERNAL_TYPE_URI);
                payload.put(PROP_TITLE, TITLE_INTERNAL);
                payload.put(PROP_STATUS, 500);
                payload.put(PROP_DETAIL, "We could not process the request at this time.");
                payload.put(PROP_INSTANCE, INSTANCE_EXAMPLE);
                payload.put(PROP_REFERENCE, "f6d2e0a1-0c39-4b52-8b4d-8d5a8fe9b4f4");
                example.setValue(payload);
                return example;
        }

        private boolean hasSchema(Components components, String name) {
                return components.getSchemas() != null && components.getSchemas().containsKey(name);
        }

        private boolean hasResponse(Components components, String name) {
                return components.getResponses() != null && components.getResponses().containsKey(name);
        }
}
