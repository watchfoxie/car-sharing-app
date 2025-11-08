package com.services.feedback_service.mapper;

import com.services.feedback_service.domain.entity.Feedback;
import com.services.feedback_service.dto.CreateFeedbackRequest;
import com.services.feedback_service.dto.FeedbackResponse;
import org.mapstruct.*;

/**
 * MapStruct mapper for {@link Feedback} entity ↔ DTOs.
 * 
 * <p>Provides type-safe mapping with null-safety for:
 * <ul>
 *   <li>Entity → Response DTO (read operations)</li>
 *   <li>Create Request DTO → Entity (write operations)</li>
 * </ul>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>Component model: Spring (managed as Spring bean)</li>
 *   <li>Unmapped target policy: IGNORE (flexible for partial updates)</li>
 *   <li>Injection strategy: CONSTRUCTOR (immutable mapper)</li>
 * </ul>
 * 
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-07
 */
@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface FeedbackMapper {

    /**
     * Maps {@link Feedback} entity to {@link FeedbackResponse} DTO.
     * 
     * <p>Used for read operations (GET endpoints).
     * 
     * @param feedback the feedback entity
     * @return feedback response DTO
     */
    FeedbackResponse toResponse(Feedback feedback);

    /**
     * Maps {@link CreateFeedbackRequest} DTO to {@link Feedback} entity.
     * 
     * <p>Used for create operations (POST /v1/feedback).
     * 
     * <p>Note: Audit fields (createdDate, createdBy) are populated by JPA auditing.
     * reviewerId must be set manually from JWT in service layer.
     * 
     * @param request the create feedback request
     * @return new feedback entity (transient, not yet persisted)
     */
    Feedback toEntity(CreateFeedbackRequest request);
}
