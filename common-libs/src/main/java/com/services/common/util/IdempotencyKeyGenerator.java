package com.services.common.util;

import java.util.UUID;

/**
 * Utility class for generating idempotency keys
 */
public final class IdempotencyKeyGenerator {
    
    private IdempotencyKeyGenerator() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    /**
     * Generate a new idempotency key
     * @return UUID-based idempotency key
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Validate idempotency key format
     * @param key the key to validate
     * @return true if valid UUID format
     */
    public static boolean isValid(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            UUID.fromString(key);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
