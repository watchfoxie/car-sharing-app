package com.usarbcs.wallet.service.repository;

import com.usarbcs.wallet.service.model.WalletPayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletPaymentRepository extends JpaRepository<WalletPayment, UUID> {
    List<WalletPayment> findTop10ByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
