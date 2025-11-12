package com.services.car_service.outbox;

import com.services.common.outbox.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for accessing CarOutboxEvent entities.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Repository
public interface CarOutboxEventRepository extends JpaRepository<CarOutboxEvent, UUID> {

    /**
     * Finds all unpublished events (status = NEW) ordered by creation timestamp.
     * <p>
     * This query leverages the partial index: idx_car_outbox_new
     * </p>
     *
     * @param status   Event status filter
     * @param pageable Pagination parameters
     * @return Page of unpublished events
     */
    Page<CarOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
