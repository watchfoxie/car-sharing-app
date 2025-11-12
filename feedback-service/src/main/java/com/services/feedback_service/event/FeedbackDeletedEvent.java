package com.services.feedback_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when feedback is deleted.
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Invalidation of car's avgRating cache</li>
 *   <li>Recalculation of car statistics</li>
 * </ul>
 * </p>
 *
 * @author Car Sharing Team
 * @since Phase 12
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class FeedbackDeletedEvent extends DomainEvent {

    /**
     * Feedback UUID that was deleted.
     */
    private String feedbackId;

    /**
     * Car UUID.
     */
    private String carId;
}
