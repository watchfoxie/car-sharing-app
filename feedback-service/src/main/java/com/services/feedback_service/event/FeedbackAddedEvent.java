package com.services.feedback_service.event;

import com.services.common.event.DomainEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when new feedback is added for a car.
 * <p>
 * This event triggers:
 * <ul>
 *   <li>Invalidation of car's avgRating cache in car-service</li>
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
public class FeedbackAddedEvent extends DomainEvent {

    /**
     * Feedback UUID.
     */
    private String feedbackId;

    /**
     * Car UUID that received the feedback.
     */
    private String carId;

    /**
     * Rental UUID this feedback is for.
     */
    private String rentalId;

    /**
     * Rating value (1-5).
     */
    private Integer rating;
}
