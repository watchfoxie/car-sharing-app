package com.services.common.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Outbox Pattern polling mechanism.
 * <p>
 * These properties control the scheduling behavior of the outbox poller job
 * that reads unpublished events from the database and publishes them to Kafka.
 * </p>
 *
 * <p><b>Configuration Example (application.yaml):</b></p>
 * <pre>
 * outbox:
 *   poller:
 *     fixed-delay: 5000      # Poll every 5 seconds
 *     initial-delay: 10000   # Wait 10 seconds before first poll
 *     batch-size: 100        # Process 100 events per batch
 * </pre>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@Component
@ConfigurationProperties(prefix = "outbox.poller")
public class OutboxProperties {

    /**
     * Fixed delay between consecutive polling cycles in milliseconds.
     * <p>
     * Default: 5000ms (5 seconds)
     * </p>
     * <p>
     * This is the time to wait after the completion of one polling cycle
     * before starting the next one.
     * </p>
     */
    private long fixedDelay = 5000L;

    /**
     * Initial delay before the first polling cycle starts in milliseconds.
     * <p>
     * Default: 10000ms (10 seconds)
     * </p>
     * <p>
     * This gives the application time to fully start up before the outbox
     * poller begins processing events.
     * </p>
     */
    private long initialDelay = 10000L;

    /**
     * Number of events to process in a single polling batch.
     * <p>
     * Default: 100
     * </p>
     * <p>
     * This controls the page size when fetching unpublished events from the
     * outbox table. Adjust based on expected event volume and Kafka throughput.
     * </p>
     */
    private int batchSize = 100;

    /**
     * Whether the outbox poller is enabled.
     * <p>
     * Default: true
     * </p>
     * <p>
     * Set to false to disable automatic event publishing (useful for testing
     * or maintenance scenarios).
     * </p>
     */
    private boolean enabled = true;
}
