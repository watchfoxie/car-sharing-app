package com.services.feedback_service.outbox;

import com.services.common.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Outbox entity for feedback-service domain events.
 * Maps to the feedback.outbox_event table.
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Entity
@Table(name = "outbox_event", schema = "feedback")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class FeedbackOutboxEvent extends OutboxEvent {
}
