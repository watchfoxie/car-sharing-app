package com.services.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Global exception handler for all microservices.
 * 
 * <p>Provides centralized exception handling with RFC 7807 Problem Details format.
 * Ensures consistent error responses across all services in the Car Sharing platform.
 * 
 * <p><strong>Supported Exception Types:</strong>
 * <ul>
 *   <li>Business exceptions (BusinessException, ResourceNotFoundException, ValidationException)</li>
 *   <li>Security exceptions (AuthenticationException, AccessDeniedException)</li>
 *   <li>Validation exceptions (MethodArgumentNotValidException, ConstraintViolationException)</li>
 *   <li>HTTP exceptions (NoResourceFoundException, HttpMessageNotReadableException)</li>
 *   <li>Generic exceptions (IllegalArgumentException, IllegalStateException, Exception)</li>
 * </ul>
 * 
 * <p><strong>RFC 7807 Compliance:</strong>
 * All error responses follow the RFC 7807 Problem Details format with:
 * <ul>
 *   <li>type - URI identifying the problem type</li>
 *   <li>title - Short summary of the problem</li>
 *   <li>status - HTTP status code</li>
 *   <li>detail - Human-readable explanation</li>
 *   <li>instance - URI of the request that caused the error</li>
 *   <li>timestamp - When the error occurred</li>
 *   <li>traceId - For distributed tracing (if available)</li>
 * </ul>
 * 
 * <p><strong>Idempotency Support:</strong>
 * When implementing idempotent operations (POST, PUT, DELETE), services should:
 * <ul>
 *   <li>Accept Idempotency-Key header (UUID format)</li>
 *   <li>Store operation results keyed by this ID</li>
 *   <li>Return cached result if key is reused within retention window</li>
 *   <li>Return 409 Conflict if duplicate operation detected</li>
 * </ul>
 * 
 * <p><strong>Example Idempotency-Key Usage:</strong>
 * <pre>
 * POST /api/v1/rentals
 * Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000
 * Content-Type: application/json
 * 
 * {
 *   "carId": 123,
 *   "startDate": "2025-11-10"
 * }
 * </pre>
 * 
 * @author Car Sharing Development Team
 * @version 1.0.0
 * @since 2025-11-05
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://carsharing.example.com/problems";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String VALIDATION_ERROR_TYPE = "/validation-error";
    private static final String VALIDATION_FAILED_TITLE = "Validation Failed";
    private static final String ERRORS_KEY = "errors";

    /**
     * Handles custom business exceptions.
     * 
     * @param ex BusinessException thrown by business logic
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 400 Bad Request
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        
        log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/business-error")
            .title("Business Rule Violation")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .extensions(Map.of("errorCode", ex.getErrorCode()))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles resource not found exceptions.
     * 
     * @param ex ResourceNotFoundException thrown when entity is not found
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 404 Not Found
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        log.warn("Resource not found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/resource-not-found")
            .title("Resource Not Found")
            .status(HttpStatus.NOT_FOUND.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handles custom validation exceptions.
     * 
     * @param ex ValidationException thrown by validation logic
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 422 Unprocessable Entity
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ProblemDetail> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        
        log.warn("Validation exception: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + VALIDATION_ERROR_TYPE)
            .title(VALIDATION_FAILED_TITLE)
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .extensions(ex.getErrors() != null ? Map.of(ERRORS_KEY, ex.getErrors()) : null)
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * Handles Bean Validation (JSR-303) constraint violations.
     * 
     * @param ex ConstraintViolationException from Bean Validation
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 422 Unprocessable Entity
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest request) {
        
        log.warn("Constraint violation: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
            .collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (existing, replacement) -> existing
            ));

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + VALIDATION_ERROR_TYPE)
            .title(VALIDATION_FAILED_TITLE)
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .detail("One or more fields have validation errors")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .extensions(Map.of(ERRORS_KEY, errors))
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * Handles method argument validation failures.
     * 
     * @param ex MethodArgumentNotValidException from @Valid annotation
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 422 Unprocessable Entity
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        
        log.warn("Method argument not valid: {}", ex.getMessage());

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + VALIDATION_ERROR_TYPE)
            .title(VALIDATION_FAILED_TITLE)
            .status(HttpStatus.UNPROCESSABLE_ENTITY.value())
            .detail("Request body validation failed")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .extensions(Map.of(ERRORS_KEY, errors))
            .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(problem);
    }

    /**
     * Handles 404 Not Found for unmapped routes.
     * 
     * @param ex NoResourceFoundException from Spring
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 404 Not Found
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest request) {
        
        log.warn("No resource found: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/not-found")
            .title("Endpoint Not Found")
            .status(HttpStatus.NOT_FOUND.value())
            .detail("The requested endpoint does not exist")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handles malformed JSON request bodies.
     * 
     * @param ex HttpMessageNotReadableException from Jackson
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 400 Bad Request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        
        log.warn("Malformed request body: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/malformed-request")
            .title("Malformed Request Body")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail("Request body could not be parsed. Please check JSON syntax.")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles method argument type mismatches.
     * 
     * @param ex MethodArgumentTypeMismatchException from Spring
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 400 Bad Request
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        
        log.warn("Argument type mismatch: {}", ex.getMessage());

        String detail = String.format("Parameter '%s' has invalid value '%s'. Expected type: %s",
            ex.getName(), ex.getValue(), 
            ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/invalid-parameter")
            .title("Invalid Parameter Type")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail(detail)
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles illegal argument exceptions.
     * 
     * @param ex IllegalArgumentException thrown by application logic
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {
        
        log.warn("Illegal argument: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/illegal-argument")
            .title("Invalid Argument")
            .status(HttpStatus.BAD_REQUEST.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles illegal state exceptions.
     * 
     * @param ex IllegalStateException thrown by application logic
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalStateException(
            IllegalStateException ex, HttpServletRequest request) {
        
        log.warn("Illegal state: {}", ex.getMessage());

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/illegal-state")
            .title("Operation Not Allowed")
            .status(HttpStatus.CONFLICT.value())
            .detail(ex.getMessage())
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Handles all uncaught exceptions.
     * 
     * @param ex Generic exception not handled by specific handlers
     * @param request HTTP request that caused the exception
     * @return RFC 7807 Problem Detail with 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenericException(
            Exception ex, HttpServletRequest request) {
        
        log.error("Unhandled exception: ", ex);

        ProblemDetail problem = ProblemDetail.builder()
            .type(PROBLEM_BASE_URI + "/internal-error")
            .title("Internal Server Error")
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .detail("An unexpected error occurred. Please try again later.")
            .instance(request.getRequestURI())
            .timestamp(Instant.now())
            .traceId(getTraceId(request))
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Extracts trace ID from request headers for distributed tracing.
     * 
     * @param request HTTP request
     * @return trace ID or null if not present
     */
    private String getTraceId(HttpServletRequest request) {
        return request.getHeader(TRACE_ID_HEADER);
    }
}
