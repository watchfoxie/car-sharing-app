package com.usarbcs.payment.service.repository;

import com.usarbcs.payment.service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByBankAccountIdOrderByCreatedAtDesc(UUID bankAccountId);
}
