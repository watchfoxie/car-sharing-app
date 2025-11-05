package com.services.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * RFC 7807 Problem Details for HTTP APIs
 * Standardized error response format across all microservices
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemDetail {
    
    /**
     * A URI reference that identifies the problem type
     */
    private String type;
    
    /**
     * A short, human-readable summary of the problem type
     */
    private String title;
    
    /**
     * The HTTP status code
     */
    private int status;
    
    /**
     * A human-readable explanation specific to this occurrence of the problem
     */
    private String detail;
    
    /**
     * A URI reference that identifies the specific occurrence of the problem
     */
    private String instance;
    
    /**
     * Timestamp when the error occurred
     */
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    /**
     * Trace ID for distributed tracing
     */
    private String traceId;
    
    /**
     * Additional properties for extending the problem details
     */
    private Map<String, Object> extensions;
}
