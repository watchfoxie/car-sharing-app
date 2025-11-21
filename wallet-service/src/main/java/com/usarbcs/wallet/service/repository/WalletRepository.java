package com.usarbcs.wallet.service.repository;

import com.usarbcs.wallet.service.model.Wallet;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    boolean existsByAccountId(UUID accountId);

    Optional<Wallet> findByAccountId(UUID accountId);

    @EntityGraph(attributePaths = {"creditCards", "payments"})
    Optional<Wallet> findDetailedById(UUID walletId);

    @EntityGraph(attributePaths = {"creditCards", "payments"})
    Optional<Wallet> findDetailedByAccountId(UUID accountId);
}
