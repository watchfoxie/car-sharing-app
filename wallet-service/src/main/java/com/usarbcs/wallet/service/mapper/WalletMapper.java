package com.usarbcs.wallet.service.mapper;

import com.usarbcs.core.details.CreditCardDto;
import com.usarbcs.core.details.PaymentDto;
import com.usarbcs.core.details.WalletDetails;
import com.usarbcs.wallet.service.dto.WalletCreditCardDto;
import com.usarbcs.wallet.service.dto.WalletDto;
import com.usarbcs.wallet.service.dto.WalletPaymentDto;
import com.usarbcs.wallet.service.model.Wallet;
import com.usarbcs.wallet.service.model.WalletCreditCard;
import com.usarbcs.wallet.service.model.WalletPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(target = "creditCards", source = "creditCards")
    @Mapping(target = "payments", source = "payments")
    WalletDto toDto(Wallet wallet);

    WalletCreditCardDto toCreditCardDto(WalletCreditCard card);

    @Mapping(target = "creditCardId", source = "creditCard.id")
    WalletPaymentDto toPaymentDto(WalletPayment payment);

    default WalletDetails toWalletDetails(Wallet wallet) {
        if (wallet == null) {
            return null;
        }
        WalletDetails details = new WalletDetails();
        details.setId(wallet.getId() != null ? wallet.getId().toString() : null);
        details.setAccountId(wallet.getAccountId() != null ? wallet.getAccountId().toString() : null);
        details.setCreatedAt(wallet.getCreatedAt());
        details.setUpdatedAt(wallet.getUpdatedAt());
        details.setUpdatedBy(wallet.getUpdatedBy());
        details.setDeleted(wallet.getDeleted());
        details.setActive(wallet.getActive());
        details.setCreditCards(mapCards(wallet.getCreditCards()));
        details.setPayments(mapPayments(wallet.getPayments()));
        return details;
    }

    default List<CreditCardDto> mapCards(List<WalletCreditCard> cards) {
        if (cards == null) {
            return Collections.emptyList();
        }
        return cards.stream()
                .filter(Objects::nonNull)
                .map(this::toCoreCreditCard)
                .collect(Collectors.toList());
    }

    default List<PaymentDto> mapPayments(List<WalletPayment> payments) {
        if (payments == null) {
            return Collections.emptyList();
        }
        return payments.stream()
                .filter(Objects::nonNull)
                .map(this::toCorePayment)
                .collect(Collectors.toList());
    }

    default CreditCardDto toCoreCreditCard(WalletCreditCard card) {
        if (card == null) {
            return null;
        }
        CreditCardDto dto = new CreditCardDto();
        dto.setId(card.getId() != null ? card.getId().toString() : null);
        dto.setHoldName(card.getHolderName());
        dto.setNumber(card.getMaskedNumber());
        dto.setExpirationDate(card.getExpirationDate());
        dto.setCvv("***");
        dto.setCreatedBy(card.getCreatedBy());
        return dto;
    }

    default PaymentDto toCorePayment(WalletPayment payment) {
        if (payment == null) {
            return null;
        }
        PaymentDto dto = new PaymentDto();
        dto.setId(payment.getId() != null ? payment.getId().toString() : null);
        dto.setCreatedAt(payment.getCreatedAt());
        dto.setCreatedBy(payment.getCreatedBy());
        dto.setUpdatedAt(payment.getUpdatedAt());
        dto.setUpdatedBy(payment.getUpdatedBy());
        dto.setAmount(payment.getAmount());
        dto.setPaymentStatus(payment.getStatus().name());
        dto.setPaymentType(payment.getPaymentType().name());
        dto.setBarCode(payment.getBarCode());
        dto.setCreditCard(toCoreCreditCard(payment.getCreditCard()));
        return dto;
    }
}
