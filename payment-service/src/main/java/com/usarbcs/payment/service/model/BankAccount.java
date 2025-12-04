package com.usarbcs.payment.service.model;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.payment.service.command.BankAccountCommand;
import com.usarbcs.payment.service.command.AccountStatusCommand;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Entity
@Table(name = "BANK_ACCOUNT")
@Getter
@Setter
public class BankAccount extends BaseEntity {

    @Column(name = "USER_ID", nullable = false, unique = true)
    private String userId;

    @Column(name = "ACCOUNT_NUMBER", nullable = false, unique = true)
    private String accountNumber;

    @Column(name = "ACCOUNT_TYPE", nullable = false)
    private String type;

    @Column(name = "STATUS", nullable = false)
    private String status;

    @Column(name = "CURRENCY", nullable = false)
    private String currency;

    @Column(name = "BALANCE", precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<CreditCard> creditCards = new ArrayList<>();

    @OneToMany(mappedBy = "bankAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("createdAt DESC")
    private List<Payment> payments = new ArrayList<>();

    public static BankAccount create(BankAccountCommand command, String generatedAccountNumber) {
        BankAccount account = new BankAccount();
        account.userId = command.getUserId();
        account.accountNumber = generatedAccountNumber;
        account.type = command.getType().toUpperCase(Locale.ROOT);
        account.status = "ACTIVE";
        account.currency = command.getCurrency().toUpperCase(Locale.ROOT);
        account.balance = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return account;
    }

    public void updateStatus(AccountStatusCommand command) {
        this.status = command.getStatus().toUpperCase(Locale.ROOT);
    }

    public void applyCredit(BigDecimal amount) {
        this.balance = balance.add(amount.setScale(2, RoundingMode.HALF_UP));
    }

    public void applyDebit(BigDecimal amount) {
        BigDecimal normalized = amount.setScale(2, RoundingMode.HALF_UP);
        if (this.balance.compareTo(normalized) < 0) {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        }
        this.balance = balance.subtract(normalized);
    }
}
