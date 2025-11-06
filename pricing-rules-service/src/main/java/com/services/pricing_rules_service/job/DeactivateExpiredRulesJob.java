package com.services.pricing_rules_service.job;

import com.services.pricing_rules_service.domain.entity.PricingRule;
import com.services.pricing_rules_service.domain.repository.PricingRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Scheduled job for deactivating expired pricing rules.
 *
 * <p>This job runs daily at 02:00 AM server time to automatically deactivate
 * pricing rules that have passed their {@code effectiveTo} timestamp. This ensures
 * that outdated pricing rules are not inadvertently used in calculations.</p>
 *
 * <p><strong>Execution Schedule:</strong></p>
 * <ul>
 *   <li><strong>Cron expression:</strong> {@code 0 0 2 * * ?}</li>
 *   <li><strong>Frequency:</strong> Daily at 02:00 AM</li>
 *   <li><strong>Timezone:</strong> Server default (configurable via {@code spring.task.scheduling.cron.time-zone})</li>
 * </ul>
 *
 * <p><strong>Job Logic:</strong></p>
 * <ol>
 *   <li>Query database for all expired pricing rules ({@code effectiveTo < now AND active = true})</li>
 *   <li>Set {@code active = false} for each expired rule</li>
 *   <li>Persist changes to database</li>
 *   <li>Evict entire "pricingRules" cache to prevent serving stale data</li>
 *   <li>Log summary: number of rules deactivated, execution time</li>
 * </ol>
 *
 * <p><strong>Why 02:00 AM?</strong></p>
 * <ul>
 *   <li>Low traffic period (minimal impact on active users)</li>
 *   <li>After midnight transitions (avoids edge cases with day boundaries)</li>
 *   <li>Before WarmUpCacheJob (03:00 AM) to ensure clean cache state</li>
 * </ul>
 *
 * <p><strong>Transaction Management:</strong></p>
 * <ul>
 *   <li>Method annotated with {@code @Transactional} for atomic updates</li>
 *   <li>If job fails, changes are rolled back (no partial updates)</li>
 *   <li>Cache eviction occurs AFTER transaction commit (via {@code @CacheEvict})</li>
 * </ul>
 *
 * <p><strong>Performance Considerations:</strong></p>
 * <ul>
 *   <li>Uses indexed query ({@code idx_pricing_rule_lookup} on {@code active} column)</li>
 *   <li>Bulk update pattern (batch processing for large datasets)</li>
 *   <li>Expected execution time: &lt;100ms for typical dataset (&lt;1000 rules)</li>
 * </ul>
 *
 * <p><strong>Example Log Output:</strong></p>
 * <pre>
 * 2025-01-09 02:00:00.123 INFO  DeactivateExpiredRulesJob - Starting scheduled job: Deactivate Expired Rules
 * 2025-01-09 02:00:00.156 INFO  DeactivateExpiredRulesJob - Found 3 expired pricing rules to deactivate
 * 2025-01-09 02:00:00.189 INFO  DeactivateExpiredRulesJob - Successfully deactivated 3 expired pricing rules in 66ms
 * </pre>
 *
 * <p><strong>Monitoring & Alerts:</strong></p>
 * <ul>
 *   <li>Job execution tracked via Spring Boot Actuator {@code /actuator/scheduledtasks}</li>
 *   <li>Errors logged at ERROR level (consider alerting on repeated failures)</li>
 *   <li>Metrics exported via Micrometer (execution count, duration, failure rate)</li>
 * </ul>
 *
 * <p><strong>Alternative Approaches Considered:</strong></p>
 * <ul>
 *   <li><strong>Database trigger:</strong> Rejected (adds DB complexity, harder to test/debug)</li>
 *   <li><strong>Lazy deactivation:</strong> Rejected (would return expired rules until query time)</li>
 *   <li><strong>TTL-based cache eviction:</strong> Rejected (cache might still serve expired rules until TTL expires)</li>
 * </ul>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see PricingRuleRepository#findExpiredRules(Instant)
 * @see Scheduled
 * @see CacheEvict
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeactivateExpiredRulesJob {

    private final PricingRuleRepository pricingRuleRepository;

    /**
     * Deactivates all pricing rules that have passed their effectiveTo timestamp.
     *
     * <p><strong>Execution:</strong> Daily at 02:00 AM (cron: "0 0 2 * * ?")</p>
     *
     * <p><strong>Steps:</strong></p>
     * <ol>
     *   <li>Fetch all expired rules (effectiveTo &lt; now AND active = true)</li>
     *   <li>Set active = false for each rule</li>
     *   <li>Save changes to database (batched)</li>
     *   <li>Evict pricingRules cache</li>
     *   <li>Log summary</li>
     * </ol>
     *
     * <p><strong>Error Handling:</strong></p>
     * <ul>
     *   <li>If exception occurs, transaction is rolled back (no partial updates)</li>
     *   <li>Error logged at ERROR level (includes stack trace)</li>
     *   <li>Job will retry on next scheduled execution (24 hours later)</li>
     * </ul>
     */
    @Scheduled(cron = "0 0 2 * * ?") // Daily at 02:00 AM
    @Transactional
    @CacheEvict(cacheNames = {"pricingRules"}, allEntries = true)
    public void deactivateExpiredRules() {
        long startTime = System.currentTimeMillis();
        log.info("Starting scheduled job: Deactivate Expired Rules");

        try {
            Instant now = Instant.now();
            List<PricingRule> expiredRules = pricingRuleRepository.findExpiredRules(now);

            if (expiredRules.isEmpty()) {
                log.info("No expired pricing rules found. Job completed successfully.");
                return;
            }

            log.info("Found {} expired pricing rules to deactivate", expiredRules.size());

            // Deactivate all expired rules
            expiredRules.forEach(rule -> {
                rule.setActive(false);
                log.debug("Deactivating pricing rule id={}, vehicleCategory={}, unit={}, effectiveTo={}",
                        rule.getId(), rule.getVehicleCategory(), rule.getUnit(), rule.getEffectiveTo());
            });

            // Save all changes in batch
            pricingRuleRepository.saveAll(expiredRules);

            long executionTime = System.currentTimeMillis() - startTime;
            log.info("Successfully deactivated {} expired pricing rules in {}ms",
                    expiredRules.size(), executionTime);

        } catch (Exception ex) {
            log.error("Failed to deactivate expired pricing rules", ex);
            throw ex; // Propagate exception to trigger transaction rollback
        }
    }
}
