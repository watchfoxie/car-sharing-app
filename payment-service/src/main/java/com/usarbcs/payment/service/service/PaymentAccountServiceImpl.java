package com.usarbcs.payment.service.service;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.payment.service.command.AccountStatusCommand;
import com.usarbcs.payment.service.command.BankAccountCommand;
import com.usarbcs.payment.service.command.CreditCardCommand;
import com.usarbcs.payment.service.command.PaymentCommand;
import com.usarbcs.payment.service.dto.BankAccountDto;
import com.usarbcs.payment.service.dto.CreditCardDto;
import com.usarbcs.payment.service.dto.PaymentRecordDto;
import com.usarbcs.payment.service.mapper.BankAccountMapper;
import com.usarbcs.payment.service.mapper.CreditCardMapper;
import com.usarbcs.payment.service.mapper.PaymentMapper;
import com.usarbcs.payment.service.model.BankAccount;
import com.usarbcs.payment.service.model.CreditCard;
import com.usarbcs.payment.service.model.Payment;
import com.usarbcs.payment.service.model.PaymentType;
import com.usarbcs.payment.service.payload.AccountDetailsPayload;
import com.usarbcs.payment.service.repository.BankAccountRepository;
import com.usarbcs.payment.service.repository.CreditCardRepository;
import com.usarbcs.payment.service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentAccountServiceImpl implements PaymentAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final CreditCardRepository creditCardRepository;
    private final PaymentRepository paymentRepository;
    private final BankAccountMapper bankAccountMapper;
    private final CreditCardMapper creditCardMapper;
    private final PaymentMapper paymentMapper;

    @Override
    public BankAccountDto createAccount(BankAccountCommand command) {
        command.validate();
        bankAccountRepository.findByUserId(command.getUserId()).ifPresent(account -> {
            throw new BusinessException(ExceptionPayloadFactory.INVALID_PAYLOAD.get());
        });
        final String accountNumber = generateAccountNumber();
        final BankAccount account = BankAccount.create(command, accountNumber);
        bankAccountRepository.save(account);
        log.info("Bank account {} created for user {}", account.getId(), command.getUserId());
        return bankAccountMapper.toDto(account);
    }

    @Override
    public BankAccountDto updateStatus(UUID accountId, AccountStatusCommand command) {
        command.validate();
        final BankAccount bankAccount = findAccountById(accountId);
        bankAccount.updateStatus(command);
        log.info("Bank account {} status updated to {}", accountId, command.getStatus());
        return bankAccountMapper.toDto(bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountDto findOne(UUID accountId) {
        return bankAccountMapper.toDto(findAccountById(accountId));
    }

    @Override
    public CreditCardDto addCard(UUID accountId, CreditCardCommand command) {
        command.validate();
        final BankAccount bankAccount = findAccountById(accountId);
        final CreditCard creditCard = CreditCard.create(command, bankAccount, generateCardToken(command));
        bankAccount.getCreditCards().add(creditCard);
        creditCardRepository.save(creditCard);
        log.info("Credit card {} added to account {}", creditCard.getId(), accountId);
        return creditCardMapper.toDto(creditCard);
    }

    @Override
    public PaymentRecordDto registerPayment(PaymentCommand command) {
        command.validate();
        final BankAccount bankAccount = findAccountById(command.getBankAccountId());
        CreditCard creditCard = null;
        if (command.getCreditCardId() != null) {
            creditCard = creditCardRepository.findByIdAndBankAccountId(command.getCreditCardId(), bankAccount.getId())
                    .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.CREDIT_CARD_NOT_FOUND.get()));
        }
        final Payment payment = Payment.create(command, bankAccount, creditCard);
        bankAccount.getPayments().add(payment);
        adjustBalance(bankAccount, payment);
        paymentRepository.save(payment);
        log.info("Payment {} recorded for account {}", payment.getId(), bankAccount.getId());
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountDetailsPayload getAccountDetails(UUID accountId) {
        final BankAccount bankAccount = findAccountById(accountId);
        final List<CreditCardDto> cards = bankAccount.getCreditCards().stream()
                .map(creditCardMapper::toDto)
                .collect(Collectors.toList());
        final List<PaymentRecordDto> payments = paymentRepository
                .findByBankAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
        return AccountDetailsPayload.builder()
                .account(bankAccountMapper.toDto(bankAccount))
                .creditCards(cards)
                .payments(payments)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentRecordDto> getPayments(UUID accountId) {
        findAccountById(accountId);
        return paymentRepository.findByBankAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(paymentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public com.usarbcs.core.details.BankAccount findByUserId(String userId) {
        final BankAccount bankAccount = bankAccountRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.BANK_ACCOUNT_NOT_FOUND.get()));
        return bankAccountMapper.toCore(bankAccount);
    }

    private BankAccount findAccountById(UUID accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new BusinessException(ExceptionPayloadFactory.BANK_ACCOUNT_NOT_FOUND.get()));
    }

    private String generateAccountNumber() {
        return "ACC-" + UUID.randomUUID();
    }

    private String generateCardToken(CreditCardCommand command) {
        return UUID.nameUUIDFromBytes((command.getNumber() + command.getCvv()).getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void adjustBalance(BankAccount account, Payment payment) {
        final BigDecimal amount = payment.getAmount();
        if (payment.getPaymentType() == PaymentType.DEBIT) {
            account.applyDebit(amount);
        } else {
            account.applyCredit(amount);
        }
    }
}
