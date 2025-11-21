package com.usarbcs.wallet.service.model;

import com.usarbcs.wallet.service.command.WalletPaymentCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Entity
@Table(name = "WALLET_PAYMENT")
@Getter
@Setter
public class WalletPayment extends BaseEntity {

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private PaymentStatus status = PaymentStatus.SETTLED;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TYPE", nullable = false)
    private PaymentType paymentType;

    @Column(name = "BAR_CODE")
    private String barCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WALLET_ID", nullable = false)
    private Wallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREDIT_CARD_ID")
    private WalletCreditCard creditCard;

    public static WalletPayment create(WalletPaymentCommand command,
                                       PaymentType paymentType,
                                       WalletCreditCard creditCard,
                                       Wallet wallet) {
        WalletPayment payment = new WalletPayment();
        payment.amount = command.getAmount().setScale(2, RoundingMode.HALF_UP);
        payment.paymentType = paymentType;
        payment.barCode = command.getBarCode();
        payment.creditCard = creditCard;
        payment.wallet = wallet;
        return payment;
    }
}
