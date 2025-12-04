package com.usarbcs.payment.service.model;

import com.usarbcs.payment.service.command.CreditCardCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;

@Entity
@Table(name = "CREDIT_CARD")
@Getter
@Setter
public class CreditCard extends BaseEntity {

    @Column(name = "HOLDER_NAME", nullable = false)
    private String holderName;

    @Column(name = "CARD_ALIAS", nullable = false)
    private String cardAlias;

    @Column(name = "LAST_FOUR", nullable = false, length = 4)
    private String lastFour;

    @Column(name = "BRAND", nullable = false)
    private String brand;

    @Column(name = "EXPIRATION_DATE", nullable = false)
    private String expirationDate;

    @Column(name = "CARD_TOKEN", nullable = false, unique = true)
    private String cardToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BANK_ACCOUNT_ID", nullable = false)
    private BankAccount bankAccount;

    public static CreditCard create(CreditCardCommand command, BankAccount bankAccount, String token) {
        CreditCard card = new CreditCard();
        card.bankAccount = bankAccount;
        card.cardAlias = command.getAlias();
        card.holderName = command.getHolderName();
        card.lastFour = command.getNumber().substring(command.getNumber().length() - 4);
        card.brand = command.getBrand().toUpperCase(Locale.ROOT);
        card.expirationDate = command.getExpirationDate();
        card.cardToken = token;
        return card;
    }
}
