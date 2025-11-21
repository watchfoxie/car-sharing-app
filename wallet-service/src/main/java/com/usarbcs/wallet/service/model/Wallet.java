package com.usarbcs.wallet.service.model;

import com.usarbcs.wallet.service.command.WalletCommand;
import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.usarbcs.core.util.Assert.assertNotNull;

@Entity
@Table(name = "WALLET")
@Getter
@Setter
public class Wallet extends BaseEntity {

    @Column(name = "ACCOUNT_ID", nullable = false, unique = true)
    private UUID accountId;

    @Column(name = "BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletCreditCard> creditCards = new ArrayList<>();

    @OneToMany(mappedBy = "wallet", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<WalletPayment> payments = new ArrayList<>();

    public static Wallet create(WalletCommand command) {
        command.validate();
        Wallet wallet = new Wallet();
        wallet.accountId = command.getAccountId();
        wallet.balance = normalize(command.getInitialBalance());
        return wallet;
    }

    public WalletPayment registerPayment(WalletPaymentCommand command,
                                         PaymentType paymentType,
                                         WalletCreditCard creditCard) {
        command.validate();
        assertNotNull(paymentType);
        WalletPayment payment = WalletPayment.create(command, paymentType, creditCard, this);
        applyBalanceMutation(paymentType, payment.getAmount());
        payments.add(payment);
        return payment;
    }

    public void addCreditCard(WalletCreditCard card) {
        assertNotNull(card);
        creditCards.add(card);
        card.setWallet(this);
    }

    private void applyBalanceMutation(PaymentType paymentType, BigDecimal amount) {
        BigDecimal normalizedAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (paymentType == PaymentType.DEBIT && balance.compareTo(normalizedAmount) < 0) {
            throw new com.usarbcs.core.exception.BusinessException(
                    com.usarbcs.core.exception.ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
        if (paymentType == PaymentType.DEBIT) {
            balance = balance.subtract(normalizedAmount);
        } else {
            balance = balance.add(normalizedAmount);
        }
    }

    private static BigDecimal normalize(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
