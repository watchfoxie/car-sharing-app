package com.usarbcs.payment.service.repository;

import com.usarbcs.payment.service.model.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BankAccountRepository extends JpaRepository<BankAccount, UUID> {
    Optional<BankAccount> findByUserId(String userId);
}
