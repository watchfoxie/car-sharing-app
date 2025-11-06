package com.services.pricing_rules_service.exception;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for the Pricing Rules Service.
 *
 * <p>This class provides centralized exception handling across all {@code @RestController}
 * endpoints, converting exceptions into RFC 7807 {@code ProblemDetail} responses with
 * consistent structure and appropriate HTTP status codes.</p>
 *
 * <p><strong>RFC 7807 Problem Details:</strong></p>
 * <p>Problem Details for HTTP APIs (RFC 7807) is a standard format for describing
 * errors in HTTP API responses. It provides a structured way to convey error information
 * to clients, including:</p>
 * <ul>
 *   <li><strong>type</strong>: URI reference identifying the problem type</li>
 *   <li><strong>title</strong>: Short, human-readable summary</li>
 *   <li><strong>status</strong>: HTTP status code</li>
 *   <li><strong>detail</strong>: Human-readable explanation</li>
 *   <li><strong>instance</strong>: URI reference identifying the specific occurrence</li>
 *   <li><strong>Additional properties</strong>: Extension fields (e.g., validation errors, timestamp)</li>
 * </ul>
 *
 * <p><strong>Exception Mapping:</strong></p>
 * <table border="1" cellpadding="5">
 *   <thead>
 *     <tr>
 *       <th>Exception Type</th>
 *       <th>HTTP Status</th>
 *       <th>Title</th>
 *       <th>Description</th>
 *     </tr>
 *   </thead>
 *   <tbody>
 *     <tr>
 *       <td>MethodArgumentNotValidException</td>
 *       <td>400 Bad Request</td>
 *       <td>Validation Failed</td>
 *       <td>Bean validation errors with field-level details</td>
 *     </tr>
 *     <tr>
 *       <td>IllegalArgumentException</td>
 *       <td>400 Bad Request</td>
 *       <td>Invalid Request</td>
 *       <td>Business logic validation errors (e.g., duration constraints)</td>
 *     </tr>
 *     <tr>
 *       <td>EntityNotFoundException</td>
 *       <td>404 Not Found</td>
 *       <td>Resource Not Found</td>
 *       <td>Requested pricing rule does not exist</td>
 *     </tr>
 *     <tr>
 *       <td>AccessDeniedException</td>
 *       <td>403 Forbidden</td>
 *       <td>Access Denied</td>
 *       <td>Insufficient permissions (role-based)</td>
 *     </tr>
 *     <tr>
 *       <td>AuthenticationException</td>
 *       <td>401 Unauthorized</td>
 *       <td>Authentication Required</td>
 *       <td>Invalid or missing JWT token</td>
 *     </tr>
 *     <tr>
 *       <td>DataIntegrityViolationException</td>
 *       <td>409 Conflict</td>
 *       <td>Data Integrity Violation</td>
 *       <td>Database constraint violations (e.g., EXCLUDE constraint)</td>
 *     </tr>
 *     <tr>
 *       <td>IllegalStateException</td>
 *       <td>500 Internal Server Error</td>
 *       <td>Internal Server Error</td>
 *       <td>Missing pricing rules or system misconfiguration</td>
 *     </tr>
 *     <tr>
 *       <td>Exception (catch-all)</td>
 *       <td>500 Internal Server Error</td>
 *       <td>Internal Server Error</td>
 *       <td>Unexpected errors</td>
 *     </tr>
 *   </tbody>
 * </table>
 *
 * <p><strong>Example Error Response (400 Validation Failed):</strong></p>
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Validation Failed",
 *   "status": 400,
 *   "detail": "Request validation failed. Check 'errors' field for details.",
 *   "instance": "/v1/pricing/rules",
 *   "timestamp": "2025-01-09T14:30:00Z",
 *   "errors": {
 *     "pricePerUnit": "Price per unit must be >= 0",
 *     "effectiveFrom": "Effective from date is required"
 *   }
 * }
 * </pre>
 *
 * <p><strong>Example Error Response (404 Not Found):</strong></p>
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Resource Not Found",
 *   "status": 404,
 *   "detail": "Pricing rule not found with id: 999",
 *   "instance": "/v1/pricing/rules/999",
 *   "timestamp": "2025-01-09T14:35:00Z"
 * }
 * </pre>
 *
 * <p><strong>Example Error Response (409 Conflict):</strong></p>
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Data Integrity Violation",
 *   "status": 409,
 *   "detail": "Pricing rule overlaps with existing rule for STANDARD category, HOUR unit",
 *   "instance": "/v1/pricing/rules",
 *   "timestamp": "2025-01-09T14:40:00Z",
 *   "constraint": "uk_pricing_rule_no_overlap"
 * }
 * </pre>
 *
 * <p><strong>Logging Strategy:</strong></p>
 * <ul>
 *   <li><strong>Client errors (4xx):</strong> WARN level (user mistake, recoverable)</li>
 *   <li><strong>Server errors (5xx):</strong> ERROR level (system issue, requires investigation)</li>
 *   <li><strong>Validation errors:</strong> DEBUG level (high frequency, low severity)</li>
 * </ul>
 *
 * @author Car Sharing Team
 * @version 1.0
 * @since 2025-11-06
 * @see ProblemDetail
 * @see RestControllerAdvice
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles Bean Validation errors (JSR-303/JSR-380).
     *
     * <p>Triggered by {@code @Valid} annotation on controller method parameters.
     * Returns 400 Bad Request with field-level validation error details.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: POST /v1/pricing/rules
     * Body: { "pricePerUnit": -10.00, "effectiveFrom": null }
     * 
     * Response: 400 Bad Request
     * {
     *   "title": "Validation Failed",
     *   "status": 400,
     *   "errors": {
     *     "pricePerUnit": "Price per unit must be >= 0",
     *     "effectiveFrom": "Effective from date is required"
     *   }
     * }
     * </pre>
     *
     * @param ex      Validation exception containing field errors
     * @param request Web request context (for extracting request path)
     * @return RFC 7807 ProblemDetail with validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        log.debug("Validation failed for request {}: {}", request.getDescription(false), ex.getMessage());

        // Extract field errors
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // Create ProblemDetail response
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Request validation failed. Check 'errors' field for details."
        );
        problemDetail.setTitle("Validation Failed");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());
        problemDetail.setProperty("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles business logic validation errors.
     *
     * <p>Triggered by service-level validations (e.g., duration constraints,
     * return &lt; pickup, invalid timestamps). Returns 400 Bad Request.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: POST /v1/pricing/calculate
     * Body: {
     *   "vehicleCategory": "STANDARD",
     *   "pickupDatetime": "2025-01-10T15:00:00Z",
     *   "returnDatetime": "2025-01-10T10:00:00Z"  // Before pickup!
     * }
     * 
     * Response: 400 Bad Request
     * {
     *   "title": "Invalid Request",
     *   "status": 400,
     *   "detail": "Return datetime must be after pickup datetime"
     * }
     * </pre>
     *
     * @param ex      Business logic validation exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        log.warn("Invalid request for {}: {}", request.getDescription(false), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                ex.getMessage()
        );
        problemDetail.setTitle("Invalid Request");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
    }

    /**
     * Handles entity not found errors (JPA).
     *
     * <p>Triggered when attempting to access a non-existent pricing rule by ID.
     * Returns 404 Not Found.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: GET /v1/pricing/rules/999
     * 
     * Response: 404 Not Found
     * {
     *   "title": "Resource Not Found",
     *   "status": 404,
     *   "detail": "Pricing rule not found with id: 999"
     * }
     * </pre>
     *
     * @param ex      Entity not found exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleEntityNotFoundException(
            EntityNotFoundException ex,
            WebRequest request) {
        log.warn("Entity not found for {}: {}", request.getDescription(false), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                ex.getMessage()
        );
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problemDetail);
    }

    /**
     * Handles authorization failures (Spring Security).
     *
     * <p>Triggered when a user attempts to access an endpoint without sufficient
     * permissions (e.g., non-ADMIN user trying to delete a rule). Returns 403 Forbidden.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: DELETE /v1/pricing/rules/42
     * User role: MANAGER (ADMIN required)
     * 
     * Response: 403 Forbidden
     * {
     *   "title": "Access Denied",
     *   "status": 403,
     *   "detail": "Insufficient permissions. Required role: ADMIN"
     * }
     * </pre>
     *
     * @param ex      Access denied exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        log.warn("Access denied for {}: {}", request.getDescription(false), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN,
                "Insufficient permissions. " + ex.getMessage()
        );
        problemDetail.setTitle("Access Denied");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problemDetail);
    }

    /**
     * Handles authentication failures (Spring Security).
     *
     * <p>Triggered when a request is made without a valid JWT token or with an
     * expired/malformed token. Returns 401 Unauthorized.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: POST /v1/pricing/calculate
     * Authorization: Bearer invalid_token_here
     * 
     * Response: 401 Unauthorized
     * {
     *   "title": "Authentication Required",
     *   "status": 401,
     *   "detail": "Invalid or missing JWT token"
     * }
     * </pre>
     *
     * @param ex      Authentication exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthenticationException(
            AuthenticationException ex,
            WebRequest request) {
        log.warn("Authentication failed for {}: {}", request.getDescription(false), ex.getMessage());

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED,
                "Invalid or missing JWT token. " + ex.getMessage()
        );
        problemDetail.setTitle("Authentication Required");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problemDetail);
    }

    /**
     * Handles database constraint violations.
     *
     * <p>Triggered when database constraints are violated (e.g., EXCLUDE constraint
     * for overlapping pricing rules, unique constraints, foreign key violations).
     * Returns 409 Conflict.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: POST /v1/pricing/rules
     * Body: {
     *   "unit": "HOUR",
     *   "vehicleCategory": "STANDARD",
     *   "effectiveFrom": "2025-01-01T00:00:00Z",
     *   "effectiveTo": "2025-12-31T23:59:59Z"
     * }
     * // Overlaps with existing rule for STANDARD/HOUR!
     * 
     * Response: 409 Conflict
     * {
     *   "title": "Data Integrity Violation",
     *   "status": 409,
     *   "detail": "Pricing rule overlaps with existing rule for STANDARD category, HOUR unit"
     * }
     * </pre>
     *
     * @param ex      Data integrity violation exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request) {
        log.warn("Data integrity violation for {}: {}", request.getDescription(false), ex.getMessage());

        // Detect EXCLUDE constraint violation (temporal overlap)
        String detail = ex.getMessage();
        if (detail != null && detail.contains("uk_pricing_rule_no_overlap")) {
            detail = "Pricing rule overlaps with existing rule for the same category and unit during the specified time period";
        } else {
            detail = "Database constraint violation. Check request data for uniqueness and referential integrity.";
        }

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                detail
        );
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
    }

    /**
     * Handles internal server errors (missing pricing rules, system misconfiguration).
     *
     * <p>Triggered by {@code IllegalStateException} thrown in service layer when
     * active pricing rules are missing for a required unit. Returns 500 Internal Server Error.</p>
     *
     * <p><strong>Example:</strong></p>
     * <pre>
     * Request: POST /v1/pricing/calculate
     * Body: {
     *   "vehicleCategory": "STANDARD",
     *   "pickupDatetime": "2025-01-10T10:00:00Z",
     *   "returnDatetime": "2025-01-10T15:00:00Z"
     * }
     * // No active HOUR pricing rule exists for STANDARD category!
     * 
     * Response: 500 Internal Server Error
     * {
     *   "title": "Internal Server Error",
     *   "status": 500,
     *   "detail": "No active pricing rule found for category=STANDARD, unit=HOUR at timestamp=2025-01-10T10:00:00Z"
     * }
     * </pre>
     *
     * @param ex      Illegal state exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(
            IllegalStateException ex,
            WebRequest request) {
        log.error("Internal server error for {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage()
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }

    /**
     * Catch-all handler for unexpected exceptions.
     *
     * <p>Handles any unhandled exceptions that don't match specific handlers above.
     * Returns 500 Internal Server Error with sanitized error message (no sensitive details).</p>
     *
     * <p><strong>Logging:</strong> Full stack trace is logged at ERROR level for debugging.</p>
     *
     * @param ex      Unhandled exception
     * @param request Web request context
     * @return RFC 7807 ProblemDetail
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex,
            WebRequest request) {
        log.error("Unexpected error for {}: {}", request.getDescription(false), ex.getMessage(), ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support if the problem persists."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setInstance(URI.create(request.getDescription(false).replace("uri=", "")));
        problemDetail.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problemDetail);
    }
}
