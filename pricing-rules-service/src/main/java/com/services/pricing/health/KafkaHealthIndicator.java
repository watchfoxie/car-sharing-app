package com.services.pricing.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Custom health indicator for Kafka broker connectivity.
 * Validates that the service can communicate with Kafka cluster for event publishing.
 * 
 * @see <a href="https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.endpoints.health.writing-custom-health-indicators">Custom Health Indicators</a>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {
    
    private final KafkaAdmin kafkaAdmin;
    private static final Duration HEALTH_CHECK_TIMEOUT = Duration.ofSeconds(5);
    
    @Override
    public Health health() {
        try (AdminClient adminClient = AdminClient.create(kafkaAdmin.getConfigurationProperties())) {
            DescribeClusterResult clusterDescription = adminClient.describeCluster();
            
            // Blocking call with timeout to check cluster connectivity
            String clusterId = clusterDescription.clusterId()
                    .get(HEALTH_CHECK_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            
            int nodeCount = clusterDescription.nodes()
                    .get(HEALTH_CHECK_TIMEOUT.toSeconds(), TimeUnit.SECONDS)
                    .size();
            
            log.trace("Kafka health check successful: clusterId={}, nodes={}", clusterId, nodeCount);
            
            return Health.up()
                    .withDetail("clusterId", clusterId)
                    .withDetail("nodes", nodeCount)
                    .withDetail("bootstrapServers", kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"))
                    .build();
                    
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Kafka health check interrupted", e);
            return Health.down()
                    .withDetail("error", "HealthCheckInterrupted")
                    .withDetail("message", e.getMessage())
                    .build();
        } catch (Exception e) {
            log.error("Kafka health check failed", e);
            return Health.down()
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
