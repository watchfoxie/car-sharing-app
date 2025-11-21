package com.usarbcs.wallet.service.service;

import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.wallet.service.command.WalletCommand;
import com.usarbcs.wallet.service.command.WalletCreditCardCommand;
import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.dto.WalletPaymentDto;
import com.usarbcs.wallet.service.mapper.WalletMapper;
import com.usarbcs.wallet.service.model.PaymentType;
import com.usarbcs.wallet.service.model.Wallet;
import com.usarbcs.wallet.service.model.WalletCreditCard;
import com.usarbcs.wallet.service.payload.WalletSnapshotPayload;
import com.usarbcs.wallet.service.repository.WalletCreditCardRepository;
import com.usarbcs.wallet.service.repository.WalletPaymentRepository;
import com.usarbcs.wallet.service.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletCreditCardRepository walletCreditCardRepository;
    private final WalletPaymentRepository walletPaymentRepository;
    private final WalletMapper walletMapper;

    @Override
    public WalletDto create(WalletCommand command) {
        command.validate();
        if (walletRepository.existsByAccountId(command.getAccountId())) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
        Wallet wallet = Wallet.create(command);
        walletRepository.save(wallet);
        log.info("Wallet {} created for account {}", wallet.getId(), wallet.getAccountId());
        return walletMapper.toDto(wallet);
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDto findById(UUID walletId) {
        return walletMapper.toDto(loadWallet(walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDto findByAccountId(UUID accountId) {
        return walletMapper.toDto(loadWalletByAccount(accountId));
    }

    @Override
    public WalletDto addCreditCard(UUID walletId, WalletCreditCardCommand command) {
        command.validate();
        Wallet wallet = loadWallet(walletId);
        String fingerprint = WalletCreditCard.fingerprintOf(command);
        walletCreditCardRepository.findByFingerprintAndWalletId(fingerprint, walletId)
                .ifPresent(card -> {
                    throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
                });
        WalletCreditCard card = WalletCreditCard.create(command, wallet);
        wallet.addCreditCard(card);
        walletRepository.save(wallet);
        log.info("Credit card {} added to wallet {}", card.getId(), walletId);
        return walletMapper.toDto(loadWallet(walletId));
    }

    @Override
    public WalletDto registerPayment(UUID walletId, WalletPaymentCommand command) {
        command.validate();
        Wallet wallet = loadWallet(walletId);
        PaymentType paymentType = PaymentType.fromValue(command.getPaymentType());
        WalletCreditCard creditCard = resolveCreditCard(walletId, command, paymentType);
        wallet.registerPayment(command, paymentType, creditCard);
        walletRepository.save(wallet);
        log.info("Payment registered for wallet {}", walletId);
        return walletMapper.toDto(loadWallet(walletId));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletSnapshotPayload snapshot(UUID walletId) {
        Wallet wallet = loadWallet(walletId);
        List<WalletPaymentDto> recentPayments = walletPaymentRepository
                .findTop10ByWalletIdOrderByCreatedAtDesc(walletId)
                .stream()
                .map(walletMapper::toPaymentDto)
            .toList();
        return WalletSnapshotPayload.builder()
                .wallet(walletMapper.toDto(wallet))
                .recentPayments(recentPayments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WalletDetails getWalletDetailsByAccountId(UUID accountId) {
        Wallet wallet = loadWalletByAccount(accountId);
        return walletMapper.toWalletDetails(wallet);
    }

    private Wallet loadWallet(UUID walletId) {
        return walletRepository.findDetailedById(walletId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.WALLET_NOT_FOUND.get()));
    }

    private Wallet loadWalletByAccount(UUID accountId) {
        return walletRepository.findDetailedByAccountId(accountId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.WALLER_FOR_ACCOUNT_NOT_FOUND.get()));
    }

    private WalletCreditCard resolveCreditCard(UUID walletId,
                                               WalletPaymentCommand command,
                                               PaymentType paymentType) {
        if (command.getCreditCardId() == null) {
            if (paymentType == PaymentType.DEBIT) {
                throw new BusinessException(ExceptionPayloadFactory.CREDIT_CARD_NOT_FOUND.get());
            }
            return null;
        }
        return walletCreditCardRepository.findByIdAndWalletId(command.getCreditCardId(), walletId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.CREDIT_CARD_NOT_FOUND.get()));
    }
}
