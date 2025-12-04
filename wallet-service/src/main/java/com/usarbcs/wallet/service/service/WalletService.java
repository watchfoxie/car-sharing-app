package com.usarbcs.wallet.service.service;

import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.wallet.service.command.WalletCommand;
import com.usarbcs.wallet.service.command.WalletCreditCardCommand;
import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.payload.WalletSnapshotPayload;

import java.util.UUID;

public interface WalletService {
    WalletDto create(WalletCommand command);

    WalletDto findById(UUID walletId);

    WalletDto findByAccountId(UUID accountId);

    WalletDto addCreditCard(UUID walletId, WalletCreditCardCommand command);

    WalletDto registerPayment(UUID walletId, WalletPaymentCommand command);

    WalletSnapshotPayload snapshot(UUID walletId);

    WalletDetails getWalletDetailsByAccountId(UUID accountId);
}
