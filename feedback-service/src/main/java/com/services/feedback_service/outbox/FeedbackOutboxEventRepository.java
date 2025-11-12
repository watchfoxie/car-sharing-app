package com.services.feedback_service.outbox;

import com.services.common.outbox.OutboxEventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for accessing FeedbackOutboxEvent entities.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Repository
public interface FeedbackOutboxEventRepository extends JpaRepository<FeedbackOutboxEvent, UUID> {

    /**
     * Finds all unpublished events (status = NEW) ordered by creation timestamp.
     *
     * @param status   Event status filter
     * @param pageable Pagination parameters
     * @return Page of unpublished events
     */
    Page<FeedbackOutboxEvent> findByStatusOrderByCreatedAtAsc(OutboxEventStatus status, Pageable pageable);
}
