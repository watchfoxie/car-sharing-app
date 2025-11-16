package com.services.identity_adapter.service;

import com.services.identity_adapter.domain.entity.Account;
import com.services.identity_adapter.domain.repository.AccountRepository;
import com.services.identity_adapter.dto.AccountProfileResponse;
import com.services.identity_adapter.dto.UpdateAccountProfileRequest;
import com.services.identity_adapter.mapper.AccountMapper;
import com.services.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user account operations.
 * 
 * <p>Handles account profile retrieval and updates. Integrates with OIDC provider
 * for authentication and synchronization.
 * 
 * <p><strong>Key operations:</strong>
 * <ul>
 *   <li>Get account profile by ID (from JWT subject)</li>
 *   <li>Update non-sensitive profile attributes</li>
 *   <li>Create/sync account from OIDC provider (future implementation)</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;

    /**
     * Get account profile by ID.
     * 
     * @param accountId the account ID (typically from JWT sub claim)
     * @return account profile response DTO
     * @throws ResourceNotFoundException if account not found
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "accountProfiles", key = "#accountId")
    public AccountProfileResponse getAccountProfile(String accountId) {
        log.debug("Fetching account profile for ID: {}", accountId);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        
        log.info("Successfully retrieved account profile for user: {}", account.getUsername());
        return accountMapper.toProfileResponse(account);
    }

    /**
     * Update account profile with non-sensitive attributes.
     * 
     * <p>Only updates fields provided in request (null values ignored).
     * Sensitive fields (username, email, enabled) cannot be updated via this method.
     * 
     * @param accountId the account ID to update
     * @param request update request with new values
     * @return updated account profile response
     * @throws ResourceNotFoundException if account not found
     */
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(cacheNames = "accountProfiles", key = "#accountId")
    public AccountProfileResponse updateAccountProfile(String accountId, UpdateAccountProfileRequest request) {
        log.debug("Updating account profile for ID: {} with request: {}", accountId, request);
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found with ID: " + accountId));
        
        // Update only non-null fields from request
        accountMapper.updateAccountFromRequest(request, account);
        
        Account updatedAccount = accountRepository.save(account);
        
        log.info("Successfully updated account profile for user: {}", updatedAccount.getUsername());
        return accountMapper.toProfileResponse(updatedAccount);
    }

    /**
     * Check if account exists by ID.
     * 
     * @param accountId the account ID to check
     * @return true if account exists
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "accountExistence", key = "'exists:' + #accountId")
    public boolean accountExists(String accountId) {
        return accountRepository.existsById(accountId);
    }

    /**
     * Check if username is available (not taken).
     * 
     * @param username the username to check (case-insensitive)
     * @return true if username is available
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "accountExistence", key = "'username:' + #username.toLowerCase()")
    public boolean isUsernameAvailable(String username) {
        return !accountRepository.existsByUsername(username);
    }

    /**
     * Check if email is available (not registered).
     * 
     * @param email the email to check (case-insensitive)
     * @return true if email is available
     */
    @Transactional(readOnly = true)
    @org.springframework.cache.annotation.Cacheable(cacheNames = "accountExistence", key = "'email:' + #email.toLowerCase()")
    public boolean isEmailAvailable(String email) {
        return !accountRepository.existsByEmail(email);
    }
}
