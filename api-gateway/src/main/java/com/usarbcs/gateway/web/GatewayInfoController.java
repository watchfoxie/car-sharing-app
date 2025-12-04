package com.usarbcs.gateway.web;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class GatewayInfoController {

    private final DiscoveryClient discoveryClient;

    private static final List<ServiceBlueprint> BLUEPRINTS = List.of(
            new ServiceBlueprint("Config Server", "CONFIG-SERVER", "Centralized externalized configuration", List.of("/actuator/**")),
            new ServiceBlueprint("Eureka Server", "EUREKA-SERVER", "Service discovery registry", List.of("/eureka/**")),
            new ServiceBlueprint("Authentication Service", "AUTH", "Reactive account provisioning", List.of("/v1/auth")),
            new ServiceBlueprint("User Service", "USER-SERVICE", "Credential validation and login workflows", List.of("/v1/auth/login", "/v1/auth/register")),
            new ServiceBlueprint("Customer Service", "CUSTOMER", "Customer lifecycle management", List.of("/v1/customers/**", "/v1/notification-customer/**")),
            new ServiceBlueprint("Driver Service", "DRIVER", "Driver roster, availability and notifications", List.of("/v1/drivers/**", "/v1/notifications/**")),
            new ServiceBlueprint("Driver Location Service", "DRIVER-LOCATION", "Live GPS tracking and snapshots", List.of("/v1/driver-location/**")),
            new ServiceBlueprint("Payment Service", "PAYMENT", "Bank accounts, cards and payment history", List.of("/v1/payment/**")),
            new ServiceBlueprint("Wallet Service", "WALLET", "In-app wallet and credit card vault", List.of("/v1/wallet/**")),
            new ServiceBlueprint("Rating Service", "RATING-SERVICE", "Driver and ride feedback", List.of("/v1/ratings/**")),
            new ServiceBlueprint("API Gateway", "API-GATEWAY", "Unified ingress, routing and security", List.of("/"))
    );

    @GetMapping({"/", "/__gateway/info"})
    public Mono<GatewayOverview> info() {
        List<ServiceSummary> services = BLUEPRINTS.stream()
                .map(this::mapToSummary)
                .toList();
        return Mono.just(new GatewayOverview("Car Sharing platform entry point", Instant.now(), services));
    }

    private ServiceSummary mapToSummary(ServiceBlueprint blueprint) {
        List<ServiceInstance> instances = discoveryClient.getInstances(blueprint.serviceId());
        List<String> uris = instances.stream().map(instance -> instance.getUri().toString()).toList();
        return new ServiceSummary(
                blueprint.name(),
                blueprint.serviceId(),
                blueprint.description(),
                blueprint.routes(),
                !instances.isEmpty(),
                uris
        );
    }

    private record ServiceBlueprint(String name, String serviceId, String description, List<String> routes) {}

    public record ServiceSummary(String name,
                                 String serviceId,
                                 String description,
                                 List<String> routes,
                                 boolean registered,
                                 List<String> instances) {}

    public record GatewayOverview(String message, Instant timestamp, List<ServiceSummary> services) {}
}
