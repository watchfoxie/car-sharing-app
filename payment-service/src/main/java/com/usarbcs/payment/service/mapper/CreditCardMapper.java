package com.usarbcs.payment.service.mapper;

import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.model.CreditCard;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CreditCardMapper {

    @Mapping(source = "cardAlias", target = "alias")
    CreditCardDto toDto(CreditCard entity);

    default com.usarbcs.core.details.CreditCardDto toCore(CreditCard entity) {
        if (entity == null) {
            return null;
        }
        com.usarbcs.core.details.CreditCardDto dto = new com.usarbcs.core.details.CreditCardDto();
        dto.setId(entity.getId() != null ? entity.getId().toString() : null);
        dto.setHoldName(entity.getHolderName());
        dto.setNumber("****" + entity.getLastFour());
        dto.setExpirationDate(entity.getExpirationDate());
        return dto;
    }
}
