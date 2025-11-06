package com.services.identity_adapter.mapper;

import com.services.identity_adapter.domain.entity.Account;
import com.services.identity_adapter.dto.AccountProfileResponse;
import com.services.identity_adapter.dto.UpdateAccountProfileRequest;
import org.mapstruct.*;

/**
 * MapStruct mapper for Account entity and DTOs.
 * 
 * <p>Provides bidirectional mapping between domain entities and DTOs for API responses/requests.
 * 
 * <p><strong>Mapping strategies:</strong>
 * <ul>
 *   <li>Entity → Response DTO: full mapping including audit fields</li>
 *   <li>Update Request → Entity: partial update (@MappingTarget), ignores null values</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AccountMapper {

    /**
     * Convert Account entity to AccountProfileResponse DTO.
     * 
     * @param account the account entity
     * @return account profile response DTO
     */
    AccountProfileResponse toProfileResponse(Account account);

    /**
     * Update existing Account entity from UpdateAccountProfileRequest.
     * 
     * <p>Only updates non-null fields from request. Does NOT update:
     * <ul>
     *   <li>id (immutable)</li>
     *   <li>username (managed by OIDC provider)</li>
     *   <li>email (requires OIDC provider verification)</li>
     *   <li>enabled (admin-only)</li>
     *   <li>audit fields (managed automatically)</li>
     * </ul>
     * 
     * @param request update request DTO
     * @param account existing account entity to update (target)
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "email", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "lastModifiedDate", ignore = true)
    @Mapping(target = "lastModifiedBy", ignore = true)
    void updateAccountFromRequest(UpdateAccountProfileRequest request, @MappingTarget Account account);
}
