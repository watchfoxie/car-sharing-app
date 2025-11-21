package com.usarbcs.payment.service.mapper;

import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.model.BankAccount;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {
	BankAccountDto toDto(BankAccount entity);

	default com.usarbcs.core.details.BankAccount toCore(BankAccount account) {
		if (account == null) {
			return null;
		}
		com.usarbcs.core.details.BankAccount dto = new com.usarbcs.core.details.BankAccount();
		dto.setId(account.getId() != null ? account.getId().toString() : null);
		dto.setUserId(account.getUserId());
		dto.setType(account.getType());
		dto.setStatus(account.getStatus());
		return dto;
	}
}
