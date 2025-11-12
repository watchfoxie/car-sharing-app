package com.services.api_gateway.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OpenAPI aggregation controller for API Gateway.
 * 
 * <p>Collects and aggregates OpenAPI specifications from all registered microservices
 * via Eureka Service Discovery. Provides unified API documentation accessible through
 * Swagger UI at the gateway level.
 * 
 * <p><strong>Endpoints:</strong>
 * <ul>
 *   <li>GET /openapi - Aggregated OpenAPI specification from all services</li>
 *   <li>GET /openapi/{service} - OpenAPI specification from specific service</li>
 * </ul>
 * 
 * <p><strong>Security:</strong> Public access in dev/staging, restricted in production
 * 
 * <p><strong>Service Discovery:</strong> Dynamically discovers services from Eureka
 * and fetches their /v1/api-docs endpoints
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Slf4j
@RestController
@RequestMapping("/openapi")
@RequiredArgsConstructor
public class OpenApiAggregationController {

    private final DiscoveryClient discoveryClient;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Business services to include in aggregation.
     * Excludes infrastructure services (discovery-service, config-service, api-gateway).
     */
    private static final List<String> BUSINESS_SERVICES = List.of(
        "identity-adapter",
        "car-service",
        "pricing-rules-service",
        "rental-service",
        "feedback-service"
    );

    // JSON field constants
    private static final String DESCRIPTION = "description";
    private static final String COMPONENTS = "components";
    private static final String SCHEMAS = "schemas";
    private static final String TAGS = "tags";

    /**
     * Aggregates OpenAPI specifications from all business microservices.
     * 
     * <p>Fetches /v1/api-docs from each service via Eureka discovery and merges them
     * into a single OpenAPI document with:
     * <ul>
     *   <li>Gateway info metadata</li>
     *   <li>All paths prefixed with /api/{service-name}</li>
     *   <li>Combined schemas and security schemes</li>
     *   <li>Service-specific tags</li>
     * </ul>
     * 
     * @return aggregated OpenAPI JSON specification
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> getAggregatedOpenApi() {
        log.info("Aggregating OpenAPI specifications from all services");

        return Flux.fromIterable(BUSINESS_SERVICES)
            .flatMap(this::fetchServiceOpenApi)
            .collectList()
            .map(this::mergeOpenApiSpecs)
            .doOnSuccess(result -> log.info("Successfully aggregated OpenAPI from {} services", BUSINESS_SERVICES.size()))
            .doOnError(error -> log.error("Failed to aggregate OpenAPI specifications", error));
    }

    /**
     * Retrieves OpenAPI specification from a specific service.
     * 
     * @param serviceName name of the service (e.g., "identity-adapter")
     * @return service-specific OpenAPI JSON specification
     */
    @GetMapping(value = "/{serviceName}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> getServiceOpenApi(@PathVariable String serviceName) {
        log.info("Fetching OpenAPI specification for service: {}", serviceName);

        if (!BUSINESS_SERVICES.contains(serviceName)) {
            return Mono.error(new IllegalArgumentException("Unknown service: " + serviceName));
        }

        return fetchServiceOpenApi(serviceName)
            .map(Map.Entry::getValue)
            .doOnSuccess(result -> log.info("Successfully fetched OpenAPI for {}", serviceName))
            .doOnError(error -> log.error("Failed to fetch OpenAPI for {}", serviceName, error));
    }

    /**
     * Fetches OpenAPI specification from a specific service via Eureka.
     * 
     * @param serviceName service name registered in Eureka
     * @return Mono containing service name and its OpenAPI JSON
     */
    private Mono<Map.Entry<String, String>> fetchServiceOpenApi(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

        if (instances.isEmpty()) {
            log.warn("No instances found for service: {}", serviceName);
            return Mono.empty();
        }

        ServiceInstance instance = instances.get(0); // Use first available instance
        String apiDocsUrl = instance.getUri().toString() + "/v1/api-docs";

        log.debug("Fetching OpenAPI from: {}", apiDocsUrl);

        return webClientBuilder.build()
            .get()
            .uri(apiDocsUrl)
            .retrieve()
            .bodyToMono(String.class)
            .map(apiDocs -> Map.entry(serviceName, apiDocs))
            .onErrorResume(error -> {
                log.error("Failed to fetch OpenAPI from {}: {}", serviceName, error.getMessage());
                return Mono.empty();
            });
    }

    /**
     * Merges multiple OpenAPI specifications into a single aggregated document.
     * 
     * <p>Merge strategy:
     * <ul>
     *   <li>Creates gateway-level info metadata</li>
     *   <li>Prefixes all paths with /api/{service-name}</li>
     *   <li>Combines components (schemas, securitySchemes, responses)</li>
     *   <li>Adds service-specific tags for organization</li>
     * </ul>
     * 
     * @param serviceSpecs list of (serviceName, openApiJson) entries
     * @return merged OpenAPI JSON string
     */
    private String mergeOpenApiSpecs(List<Map.Entry<String, String>> serviceSpecs) {
        try {
            ObjectNode aggregated = createAggregatedRoot();
            
            ObjectNode paths = aggregated.putObject("paths");
            ObjectNode components = aggregated.putObject(COMPONENTS);
            ObjectNode schemas = components.putObject(SCHEMAS);
            ObjectNode securitySchemes = components.putObject("securitySchemes");
            ObjectNode responses = components.putObject("responses");
            ArrayNode tags = aggregated.putArray(TAGS);

            // Merge each service specification
            for (Map.Entry<String, String> entry : serviceSpecs) {
                mergeServiceSpec(entry.getKey(), entry.getValue(), paths, schemas, 
                    securitySchemes, responses, tags);
            }

            addGlobalSecurity(aggregated);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(aggregated);

        } catch (Exception e) {
            log.error("Failed to merge OpenAPI specifications", e);
            return "{}";
        }
    }

    /**
     * Creates the root aggregated OpenAPI document with metadata.
     */
    private ObjectNode createAggregatedRoot() {
        ObjectNode aggregated = objectMapper.createObjectNode();
        aggregated.put("openapi", "3.0.1");

        ObjectNode info = aggregated.putObject("info");
        info.put("title", "Car Sharing Platform API");
        info.put(DESCRIPTION, "Unified API documentation for all microservices");
        info.put("version", "1.0.0");

        ObjectNode contact = info.putObject("contact");
        contact.put("name", "Car Sharing Development Team");
        contact.put("email", "dev@carsharing.example.com");

        ArrayNode servers = aggregated.putArray("servers");
        ObjectNode server = servers.addObject();
        server.put("url", "http://localhost:8080");
        server.put(DESCRIPTION, "API Gateway");

        return aggregated;
    }

    /**
     * Merges a single service specification into the aggregated document.
     */
    private void mergeServiceSpec(String serviceName, String openApiJson, 
                                  ObjectNode paths, ObjectNode schemas,
                                  ObjectNode securitySchemes, ObjectNode responses,
                                  ArrayNode tags) {
        try {
            JsonNode serviceSpec = objectMapper.readTree(openApiJson);

            addServiceTag(tags, serviceName);
            mergePaths(serviceSpec, serviceName, paths);
            mergeSchemas(serviceSpec, serviceName, schemas);
            mergeSecuritySchemes(serviceSpec, securitySchemes);
            mergeResponses(serviceSpec, responses);

        } catch (Exception e) {
            log.error("Failed to parse OpenAPI for service: {}", serviceName, e);
        }
    }

    /**
     * Adds service tag to the tags array.
     */
    private void addServiceTag(ArrayNode tags, String serviceName) {
        ObjectNode tag = tags.addObject();
        tag.put("name", serviceName);
        tag.put(DESCRIPTION, getServiceDescription(serviceName));
    }

    /**
     * Merges paths from service spec with /api/{service} prefix.
     */
    private void mergePaths(JsonNode serviceSpec, String serviceName, ObjectNode paths) {
        JsonNode servicePaths = serviceSpec.path("paths");
        if (servicePaths.isObject()) {
            servicePaths.fieldNames().forEachRemaining(originalPath -> {
                String prefixedPath = "/api/" + serviceName + originalPath;
                JsonNode pathItem = servicePaths.get(originalPath);
                
                if (pathItem.isObject()) {
                    ObjectNode pathItemCopy = pathItem.deepCopy();
                    addServiceTagToOperations(pathItemCopy, serviceName);
                    paths.set(prefixedPath, pathItemCopy);
                }
            });
        }
    }

    /**
     * Merges schemas from service spec with service name prefix.
     */
    private void mergeSchemas(JsonNode serviceSpec, String serviceName, ObjectNode schemas) {
        JsonNode serviceSchemas = serviceSpec.path(COMPONENTS).path(SCHEMAS);
        if (serviceSchemas.isObject()) {
            serviceSchemas.fieldNames().forEachRemaining(schemaName -> {
                String prefixedName = serviceName + "_" + schemaName;
                schemas.set(prefixedName, serviceSchemas.get(schemaName));
            });
        }
    }

    /**
     * Merges security schemes from service spec (avoids duplicates).
     */
    private void mergeSecuritySchemes(JsonNode serviceSpec, ObjectNode securitySchemes) {
        JsonNode serviceSecuritySchemes = serviceSpec.path(COMPONENTS).path("securitySchemes");
        if (serviceSecuritySchemes.isObject()) {
            serviceSecuritySchemes.fieldNames().forEachRemaining(schemeName -> {
                if (!securitySchemes.has(schemeName)) {
                    securitySchemes.set(schemeName, serviceSecuritySchemes.get(schemeName));
                }
            });
        }
    }

    /**
     * Merges responses from service spec (avoids duplicates).
     */
    private void mergeResponses(JsonNode serviceSpec, ObjectNode responses) {
        JsonNode serviceResponses = serviceSpec.path(COMPONENTS).path("responses");
        if (serviceResponses.isObject()) {
            serviceResponses.fieldNames().forEachRemaining(responseName -> {
                if (!responses.has(responseName)) {
                    responses.set(responseName, serviceResponses.get(responseName));
                }
            });
        }
    }

    /**
     * Adds global security requirement to aggregated spec.
     */
    private void addGlobalSecurity(ObjectNode aggregated) {
        ArrayNode security = aggregated.putArray("security");
        ObjectNode bearerAuth = security.addObject();
        bearerAuth.putArray("bearerAuth");
    }

    /**
     * Adds service tag to all operations in a path item.
     */
    private void addServiceTagToOperations(ObjectNode pathItem, String serviceName) {
        List<String> httpMethods = List.of("get", "post", "put", "delete", "patch", "options", "head");
        
        for (String method : httpMethods) {
            JsonNode operation = pathItem.path(method);
            if (operation.isObject()) {
                addTagToOperation((ObjectNode) operation, serviceName);
            }
        }
    }

    /**
     * Adds service tag to a single operation.
     */
    private void addTagToOperation(ObjectNode operation, String serviceName) {
        ArrayNode tagsArray = operation.has(TAGS) 
            ? (ArrayNode) operation.get(TAGS)
            : operation.putArray(TAGS);
        
        if (!hasServiceTag(tagsArray, serviceName)) {
            tagsArray.insert(0, serviceName);
        }
    }

    /**
     * Checks if service tag is already present in tags array.
     */
    private boolean hasServiceTag(ArrayNode tagsArray, String serviceName) {
        for (JsonNode tag : tagsArray) {
            if (tag.asText().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns human-readable description for a service.
     */
    private String getServiceDescription(String serviceName) {
        return switch (serviceName) {
            case "identity-adapter" -> "Identity and Access Management";
            case "car-service" -> "Car Inventory and Management";
            case "pricing-rules-service" -> "Pricing Engine and Rules";
            case "rental-service" -> "Rental Orchestration and FSM";
            case "feedback-service" -> "Feedback and Analytics";
            default -> serviceName;
        };
    }
}
