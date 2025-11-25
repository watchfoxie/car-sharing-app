package com.usarbcs.rating.exception;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayload;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.exception.MessageSourceHandler;
import com.usarbcs.core.exception.TechnicalException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RFC 7807 aligned exception handler that publishes Problem Details payloads for the rating-service REST API.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class Rfc7807ExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://car-sharing.app/problems";
    private static final URI VALIDATION_TYPE = URI.create(PROBLEM_BASE_URI + "/request-validation");
    private static final URI BUSINESS_TYPE = URI.create(PROBLEM_BASE_URI + "/business-rule-violation");
    private static final URI NOT_FOUND_TYPE = URI.create(PROBLEM_BASE_URI + "/resource-not-found");
    private static final URI INTERNAL_TYPE = URI.create(PROBLEM_BASE_URI + "/internal-error");
    private static final String HTTP_STATUS_TYPE_TEMPLATE = PROBLEM_BASE_URI + "/http-%d";
    private static final String PROP_ERRORS = "errors";
    private static final String PROP_ERROR_CODE = "errorCode";
    private static final String PROP_REFERENCE = "reference";
    private static final String TITLE_VALIDATION = "Request validation failed";
    private static final String TITLE_CONSTRAINT = "Constraint violation";
    private static final String TITLE_BUSINESS = "Business rule violation";
    private static final String TITLE_NOT_FOUND = "Resource not found";
    private static final String TITLE_INTERNAL = "Unexpected internal error";

    private final MessageSourceHandler messageSourceHandler;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                      HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
            TITLE_VALIDATION,
            "Payload validation failed. See the errors property for details.", request);
        problem.setProperty(PROP_ERRORS, groupFieldErrors(ex.getBindingResult().getFieldErrors()));
        log.debug("Validation failed for {} with payload errors {}", request.getRequestURI(), problem.getProperties());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException ex, HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
            TITLE_CONSTRAINT,
                "Request parameters did not satisfy validation constraints.", request);
        problem.setProperty(PROP_ERRORS, groupFieldErrors(ex.getBindingResult().getFieldErrors()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex,
                                                                            HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
            TITLE_CONSTRAINT,
                "Request parameters did not satisfy validation constraints.", request);
        problem.setProperty(PROP_ERRORS, groupConstraintViolations(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex,
                                                                 HttpServletRequest request) {
        ExceptionPayload payload = ex.getPayload();
        HttpStatus status = payload.getStatus() == null ? HttpStatus.CONFLICT : payload.getStatus();
        boolean notFound = HttpStatus.NOT_FOUND.equals(status);
        URI type = notFound ? NOT_FOUND_TYPE : BUSINESS_TYPE;
        String title = notFound ? TITLE_NOT_FOUND : TITLE_BUSINESS;
        String detail = messageSourceHandler.getMessage(payload.getMessage(), payload.getArgs());
        ProblemDetail problem = createProblem(status, type, title, detail, request);
        problem.setProperty(PROP_ERROR_CODE, payload.getCode());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
                                                                               HttpServletRequest request) {
        ExceptionPayload payload = ExceptionPayloadFactory.MISSING_REQUEST_BODY_ERROR_CODE.get();
        String detail = messageSourceHandler.getMessage(payload.getMessage());
        ProblemDetail problem = createProblem(payload.getStatus(), VALIDATION_TYPE,
            "Malformed request body", detail, request);
        problem.setProperty(PROP_ERROR_CODE, payload.getCode());
        return ResponseEntity.status(payload.getStatus()).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex,
                                                                       HttpServletRequest request) {
        HttpStatus status = resolveStatus(ex.getStatusCode());
        URI type = URI.create(String.format(HTTP_STATUS_TYPE_TEMPLATE, status.value()));
        String title = status.getReasonPhrase();
        String detail = ex.getReason() != null ? ex.getReason() : title;
        ProblemDetail problem = createProblem(status, type, title, detail, request);
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ProblemDetail> handleMissingResource(Exception ex, HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.NOT_FOUND, NOT_FOUND_TYPE,
                TITLE_NOT_FOUND, ex.getMessage() != null ? ex.getMessage() : TITLE_NOT_FOUND, request);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    @ExceptionHandler({TechnicalException.class, Throwable.class})
    public ResponseEntity<ProblemDetail> handleThrowable(Throwable throwable, HttpServletRequest request) {
        String reference = UUID.randomUUID().toString();
        String detail = messageSourceHandler.getMessage(ExceptionPayloadFactory.TECHNICAL_ERROR.getMessage());
        ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_TYPE,
            TITLE_INTERNAL, detail, request);
        problem.setProperty(PROP_REFERENCE, reference);
        log.error("Unexpected error {} while processing {}", reference, request.getRequestURI(), throwable);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(HttpStatus status, URI type, String title, String detail,
                                        HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setInstance(resolveInstance(request));
        return problem;
    }

    private Map<String, List<String>> groupFieldErrors(List<FieldError> fieldErrors) {
        return fieldErrors.stream()
                .collect(Collectors.groupingBy(FieldError::getField, LinkedHashMap::new,
                        Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())));
    }

    private Map<String, List<String>> groupConstraintViolations(ConstraintViolationException ex) {
        return ex.getConstraintViolations().stream()
                .collect(Collectors.groupingBy(violation -> violation.getPropertyPath().toString(), LinkedHashMap::new,
                        Collectors.mapping(ConstraintViolation::getMessage, Collectors.toList())));
    }

    private HttpStatus resolveStatus(HttpStatusCode statusCode) {
        HttpStatus resolved = HttpStatus.resolve(statusCode.value());
        return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private URI resolveInstance(HttpServletRequest request) {
        try {
            return ServletUriComponentsBuilder.fromRequestUri(request).build().toUri();
        } catch (IllegalStateException ignored) {
            return URI.create(request.getRequestURI());
        }
    }
}
