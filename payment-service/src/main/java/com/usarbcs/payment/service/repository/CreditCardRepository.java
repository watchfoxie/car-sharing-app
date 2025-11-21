package com.usarbcs.payment.service.repository;

import com.usarbcs.payment.service.model.CreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CreditCardRepository extends JpaRepository<CreditCard, UUID> {
    Optional<CreditCard> findByIdAndBankAccountId(UUID id, UUID bankAccountId);
}
