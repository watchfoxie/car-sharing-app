package com.usarbcs.wallet.service.model;

import com.usarbcs.wallet.service.command.WalletCreditCardCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "WALLET_CREDIT_CARD")
@Getter
@Setter
public class WalletCreditCard extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "WALLET_ID", nullable = false)
    private Wallet wallet;

    @Column(name = "HOLDER_NAME", nullable = false)
    private String holderName;

    @Column(name = "ALIAS")
    private String alias;

    @Column(name = "CARD_BRAND")
    private String brand;

    @Column(name = "LAST_FOUR", length = 4, nullable = false)
    private String lastFour;

    @Column(name = "MASKED_NUMBER", nullable = false)
    private String maskedNumber;

    @Column(name = "EXPIRATION_DATE", nullable = false)
    private String expirationDate;

    @Column(name = "FINGERPRINT", nullable = false)
    private String fingerprint;

    public static WalletCreditCard create(WalletCreditCardCommand command, Wallet wallet) {
        WalletCreditCard card = new WalletCreditCard();
        card.wallet = wallet;
        card.holderName = command.getHolderName();
        card.alias = command.getAlias();
        card.brand = detectBrand(command.getNumber());
        card.lastFour = extractLastFour(command.getNumber());
        card.maskedNumber = maskNumber(command.getNumber());
        card.expirationDate = command.getExpirationDate();
        card.fingerprint = fingerprintOf(command);
        return card;
    }

    public static String fingerprintOf(WalletCreditCardCommand command) {
        String source = command.getNumber() + "|" + command.getExpirationDate() + "|" +
                command.getHolderName().toUpperCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private static String detectBrand(String number) {
        if (number == null || number.isBlank()) {
            return "UNKNOWN";
        }
        if (number.startsWith("4")) {
            return "VISA";
        }
        if (number.startsWith("5")) {
            return "MASTERCARD";
        }
        if (number.startsWith("3")) {
            return "AMEX";
        }
        if (number.startsWith("6")) {
            return "DISCOVER";
        }
        return "UNKNOWN";
    }

    private static String extractLastFour(String number) {
        if (number == null || number.length() < 4) {
            return "0000";
        }
        return number.substring(number.length() - 4);
    }

    private static String maskNumber(String number) {
        if (number == null || number.length() < 4) {
            return "****";
        }
        String lastFour = extractLastFour(number);
        return "**** **** **** " + lastFour;
    }

}
