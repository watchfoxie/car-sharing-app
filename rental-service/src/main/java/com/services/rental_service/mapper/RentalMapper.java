package com.services.rental_service.mapper;

import com.services.rental_service.domain.entity.Rental;
import com.services.rental_service.dto.CreateRentalRequest;
import com.services.rental_service.dto.RentalResponse;
import org.mapstruct.*;

/**
 * MapStruct mapper for converting between {@link Rental} entities and DTOs.
 * <p>
 * Handles bi-directional mapping with null-safe partial updates.
 * All mappings ignore audit fields (managed by JPA auditing).
 * </p>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-01-09
 */
@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface RentalMapper {

    /**
     * Convert Rental entity to RentalResponse DTO.
     * <p>
     * Includes all fields: business data + audit information.
     * </p>
     *
     * @param rental the entity to convert
     * @return RentalResponse DTO, or null if input is null
     */
    RentalResponse toResponse(Rental rental);

    /**
     * Convert CreateRentalRequest DTO to new Rental entity.
     * <p>
     * Maps only business fields; audit fields are populated by JPA auditing.
     * Status defaults to PENDING.
     * </p>
     *
     * @param request the DTO to convert
     * @return new Rental entity, or null if input is null
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    @Mapping(target = "estimatedCost", ignore = true)
    @Mapping(target = "finalCost", ignore = true)
    @Mapping(target = "renterId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    Rental toEntity(CreateRentalRequest request);
}
