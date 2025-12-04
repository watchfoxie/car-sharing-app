package com.usarbcs.payment.service.model;

import com.usarbcs.payment.service.command.PaymentCommand;
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
@Table(name = "PAYMENT")
@Getter
@Setter
public class Payment extends BaseEntity {

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "BAR_CODE")
    private String barCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false)
    private PaymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "PAYMENT_TYPE", nullable = false)
    private PaymentType paymentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BANK_ACCOUNT_ID", nullable = false)
    private BankAccount bankAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CREDIT_CARD_ID")
    private CreditCard creditCard;

    public static Payment create(PaymentCommand command, BankAccount bankAccount, CreditCard creditCard) {
        Payment payment = new Payment();
        payment.amount = command.getAmount().setScale(2, RoundingMode.HALF_UP);
        payment.barCode = command.getBarCode();
        payment.status = PaymentStatus.SETTLED;
        payment.paymentType = PaymentType.fromValue(command.getPaymentType());
        payment.bankAccount = bankAccount;
        payment.creditCard = creditCard;
        return payment;
    }
}
