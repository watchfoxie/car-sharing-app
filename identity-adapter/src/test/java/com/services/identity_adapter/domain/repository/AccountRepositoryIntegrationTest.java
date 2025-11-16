package com.services.identity_adapter.domain.repository;

import com.services.identity_adapter.domain.entity.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import com.services.identity_adapter.testcontainers.PostgresTestContainer;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link AccountRepository} with PostgreSQL Testcontainers.
 * 
 * <p>Tests Flyway migrations, citext case-insensitivity, and repository methods.
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@SuppressWarnings("resource")
class AccountRepositoryIntegrationTest extends PostgresTestContainer {

    @Autowired
    private AccountRepository accountRepository;

    private Account testAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        accountRepository.flush();
        
        testAccount = Account.builder()
                .id("auth0|test123")
                .username("john.doe")
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .phoneNumber("+40712345678")
                .enabled(true)
                .build();
    }

    private Account persistDefaultAccount() {
        return accountRepository.saveAndFlush(testAccount);
    }

    @Test
    @DisplayName("Should save and retrieve account successfully")
    void shouldSaveAndRetrieveAccount() {
        // When
        Account savedAccount = accountRepository.save(testAccount);
        
        // Then
        assertThat(savedAccount.getId()).isEqualTo("auth0|test123");
        assertThat(savedAccount.getUsername()).isEqualTo("john.doe");
        assertThat(savedAccount.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(savedAccount.getCreatedDate()).isNotNull();
        assertThat(savedAccount.getCreatedBy()).isEqualTo("system");
    }

    @Test
    @DisplayName("Should find account by username case-insensitively (citext)")
    void shouldFindByUsernameCaseInsensitive() {
        // Given
        persistDefaultAccount();
        
        // When - search with different case
        Optional<Account> foundUpper = accountRepository.findByUsername("JOHN.DOE");
        Optional<Account> foundLower = accountRepository.findByUsername("john.doe");
        Optional<Account> foundMixed = accountRepository.findByUsername("John.Doe");
        
        // Then - all should find the same account
        assertThat(foundUpper).isPresent();
        assertThat(foundLower).isPresent();
        assertThat(foundMixed).isPresent();
        assertThat(foundUpper.get().getId()).isEqualTo(testAccount.getId());
        assertThat(foundLower.get().getId()).isEqualTo(testAccount.getId());
        assertThat(foundMixed.get().getId()).isEqualTo(testAccount.getId());
    }

    @Test
    @DisplayName("Should find account by email case-insensitively (citext)")
    void shouldFindByEmailCaseInsensitive() {
        // Given
        persistDefaultAccount();
        
        // When - search with different case
        Optional<Account> foundUpper = accountRepository.findByEmail("JOHN.DOE@EXAMPLE.COM");
        Optional<Account> foundLower = accountRepository.findByEmail("john.doe@example.com");
        Optional<Account> foundMixed = accountRepository.findByEmail("John.Doe@Example.Com");
        
        // Then - all should find the same account
        assertThat(foundUpper).isPresent();
        assertThat(foundLower).isPresent();
        assertThat(foundMixed).isPresent();
        assertThat(foundUpper.get().getId()).isEqualTo(testAccount.getId());
    }

    @Test
    @DisplayName("Should check username existence case-insensitively")
    void shouldCheckUsernameExistsCaseInsensitive() {
        // Given
        persistDefaultAccount();
        
        // When/Then - all case variations should return true
        assertThat(accountRepository.existsByUsername("john.doe")).isTrue();
        assertThat(accountRepository.existsByUsername("JOHN.DOE")).isTrue();
        assertThat(accountRepository.existsByUsername("John.Doe")).isTrue();
        assertThat(accountRepository.existsByUsername("jane.doe")).isFalse();
    }

    @Test
    @DisplayName("Should check email existence case-insensitively")
    void shouldCheckEmailExistsCaseInsensitive() {
        // Given
        persistDefaultAccount();
        
        // When/Then - all case variations should return true
        assertThat(accountRepository.existsByEmail("john.doe@example.com")).isTrue();
        assertThat(accountRepository.existsByEmail("JOHN.DOE@EXAMPLE.COM")).isTrue();
        assertThat(accountRepository.existsByEmail("John.Doe@Example.Com")).isTrue();
        assertThat(accountRepository.existsByEmail("jane.doe@example.com")).isFalse();
    }

    @Test
    @DisplayName("Should find enabled accounts only")
    void shouldFindEnabledAccountsOnly() {
        // Given
        testAccount.setEnabled(false);
        persistDefaultAccount();
        
        // When
        Optional<Account> enabledFound = accountRepository.findEnabledByUsername("john.doe");
        
        // Then
        assertThat(enabledFound).isEmpty();
        
        // Update to enabled
        testAccount.setEnabled(true);
        persistDefaultAccount();
        
        enabledFound = accountRepository.findEnabledByUsername("john.doe");
        assertThat(enabledFound).isPresent();
    }

    @Test
    @DisplayName("Should allow multiple NULL emails (PostgreSQL UNIQUE allows multiple NULLs)")
    void shouldAllowMultipleNullEmails() {
        // Given
        Account account1 = Account.builder()
                .id("auth0|test1")
                .username("user1")
                .email(null)
                .enabled(true)
                .build();
        
        Account account2 = Account.builder()
                .id("auth0|test2")
                .username("user2")
                .email(null)
                .enabled(true)
                .build();
        
        // When
        accountRepository.saveAndFlush(account1);
        accountRepository.saveAndFlush(account2);
        
        // Then - both should save successfully
        assertThat(accountRepository.findById("auth0|test1")).isPresent();
        assertThat(accountRepository.findById("auth0|test2")).isPresent();
    }

    @Test
    @DisplayName("Should update last modified fields on update")
    void shouldUpdateLastModifiedFieldsOnUpdate() {
        // Given
        Account savedAccount = persistDefaultAccount();
        assertThat(savedAccount.getLastModifiedDate()).isNull();
        
        // When - update account
        savedAccount.setFirstName("Jane");
        Account updatedAccount = accountRepository.saveAndFlush(savedAccount);
        
        // Then
        assertThat(updatedAccount.getFirstName()).isEqualTo("Jane");
        assertThat(updatedAccount.getLastModifiedDate()).isNotNull();
    }
}
