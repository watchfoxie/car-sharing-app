package com.usarbcs.wallet.service.repository;

import com.usarbcs.wallet.service.model.WalletCreditCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletCreditCardRepository extends JpaRepository<WalletCreditCard, UUID> {
    Optional<WalletCreditCard> findByIdAndWalletId(UUID cardId, UUID walletId);
    Optional<WalletCreditCard> findByFingerprintAndWalletId(String fingerprint, UUID walletId);
}
