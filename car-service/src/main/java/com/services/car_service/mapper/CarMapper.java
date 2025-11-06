package com.services.car_service.mapper;

import com.services.car_service.domain.entity.Car;
import com.services.car_service.dto.CarResponse;
import com.services.car_service.dto.CreateCarRequest;
import com.services.car_service.dto.UpdateCarRequest;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between Car entities and DTOs.
 * 
 * <p>Responsibilities:
 * <ul>
 *   <li>Entity → DTO (toResponse) for API responses</li>
 *   <li>DTO → Entity (toEntity) for creation</li>
 *   <li>Partial update (updateCarFromRequest) for PATCH operations</li>
 * </ul>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>Component model: Spring (managed as a Spring bean)</li>
 *   <li>Unmapped target policy: IGNORE (flexible for partial updates)</li>
 *   <li>Null value property mapping: IGNORE (preserve existing values on partial update)</li>
 * </ul>
 * 
 * @see Car
 * @see CarResponse
 * @see CreateCarRequest
 * @see UpdateCarRequest
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 */
@Mapper(componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CarMapper {

    /**
     * Converts a Car entity to a CarResponse DTO.
     * 
     * <p>The avgRating field is set to null by default; 
     * it should be populated separately by calling feedback-service.
     *
     * @param car the source entity
     * @return the response DTO
     */
    @Mapping(target = "avgRating", ignore = true)
    CarResponse toResponse(Car car);

    /**
     * Converts a CreateCarRequest DTO to a Car entity.
     * 
     * <p>The owner ID and audit fields are populated separately
     * by the service layer and JPA auditing.
     *
     * @param request the creation request DTO
     * @return the new Car entity (not persisted yet)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "archived", constant = "false")
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    Car toEntity(CreateCarRequest request);

    /**
     * Updates an existing Car entity from an UpdateCarRequest DTO.
     * 
     * <p>Only non-null fields in the request are applied (partial update).
     * The owner ID and primary key are never updated.
     *
     * @param request the update request DTO
     * @param car the existing entity to update (modified in place)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    void updateCarFromRequest(UpdateCarRequest request, @MappingTarget Car car);
}
