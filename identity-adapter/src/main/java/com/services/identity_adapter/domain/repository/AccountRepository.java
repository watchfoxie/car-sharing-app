package com.services.identity_adapter.domain.repository;

import com.services.identity_adapter.domain.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for {@link Account} entity.
 * 
 * <p>Provides CRUD operations and custom queries for user account management.
 * 
 * <p><strong>Custom queries:</strong>
 * <ul>
 *   <li>{@link #findByUsername(String)} - case-insensitive lookup via citext</li>
 *   <li>{@link #findByEmail(String)} - case-insensitive lookup via citext</li>
 *   <li>{@link #existsByUsername(String)} - check username availability</li>
 *   <li>{@link #existsByEmail(String)} - check email availability</li>
 * </ul>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Find account by username (case-insensitive).
     * 
     * <p>Leverages citext column type for case-insensitive matching.
     * 
     * @param username the username to search (case does not matter)
     * @return Optional containing account if found
     */
    Optional<Account> findByUsername(String username);

    /**
     * Find account by email (case-insensitive).
     * 
     * <p>Leverages citext column type for case-insensitive matching.
     * 
     * @param email the email to search (case does not matter)
     * @return Optional containing account if found
     */
    Optional<Account> findByEmail(String email);

    /**
     * Check if username exists (case-insensitive).
     * 
     * @param username the username to check
     * @return true if username is already taken
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists (case-insensitive).
     * 
     * @param email the email to check
     * @return true if email is already registered
     */
    boolean existsByEmail(String email);

    /**
     * Find enabled accounts by username.
     * 
     * @param username the username to search
     * @return Optional containing enabled account if found
     */
    @Query("SELECT a FROM Account a WHERE a.username = :username AND a.enabled = true")
    Optional<Account> findEnabledByUsername(String username);

    /**
     * Find enabled accounts by email.
     * 
     * @param email the email to search
     * @return Optional containing enabled account if found
     */
    @Query("SELECT a FROM Account a WHERE a.email = :email AND a.enabled = true")
    Optional<Account> findEnabledByEmail(String email);
}
