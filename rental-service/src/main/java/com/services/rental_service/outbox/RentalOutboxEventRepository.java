package com.services.rental_service.outbox;

import com.services.common.outbox.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for accessing RentalOutboxEvent entities.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Repository
public interface RentalOutboxEventRepository extends JpaRepository<RentalOutboxEvent, UUID> {

    /**
     * Finds all unpublished events (status = NEW) ordered by creation timestamp.
     *
     * @param status   Event status filter
     * @param pageable Pagination parameters
     * @return Page of unpublished events
     */
    Page<RentalOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
