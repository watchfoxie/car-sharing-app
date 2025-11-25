package com.usarbcs.driver.exception;

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
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RFC 7807 aligned exception handler that publishes Problem Details payloads for the driver-service REST API.
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class Rfc7807ExceptionHandler {

    private static final String PROBLEM_BASE_URI = "https://car-sharing.app/problems";
    private static final URI VALIDATION_TYPE = URI.create(PROBLEM_BASE_URI + "/request-validation");
    private static final URI BUSINESS_TYPE = URI.create(PROBLEM_BASE_URI + "/business-rule-violation");
    private static final URI INTERNAL_TYPE = URI.create(PROBLEM_BASE_URI + "/internal-error");
    private static final String HTTP_STATUS_TYPE_TEMPLATE = PROBLEM_BASE_URI + "/http-%d";
    private static final String PROP_ERRORS = "errors";
    private static final String PROP_ERROR_CODE = "errorCode";
    private static final String PROP_REFERENCE = "reference";
    private static final String TITLE_NOT_FOUND = "Resource not found";
    private static final String DETAIL_NOT_FOUND = "Requested resource could not be found.";

    private final MessageSourceHandler messageSourceHandler;

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
                                                                               HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Request validation failed",
                "Payload validation failed. See the errors property for details.", request);
        problem.setProperty(PROP_ERRORS, groupFieldErrors(ex.getBindingResult().getFieldErrors()));
        log.debug("Validation failed for {} with payload errors {}", request.getRequestURI(), problem.getProperties());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ProblemDetail> handleBindException(BindException ex, HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Request binding failed",
                "Request parameters did not satisfy validation constraints.", request);
        problem.setProperty(PROP_ERRORS, groupFieldErrors(ex.getFieldErrors()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex,
                                                                            HttpServletRequest request) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Constraint violation",
                "Request parameters did not satisfy validation constraints.", request);
        problem.setProperty(PROP_ERRORS, groupConstraintViolations(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ExceptionPayload payload = ex.getPayload();
        HttpStatus status = payload.getStatus() == null ? HttpStatus.CONFLICT : payload.getStatus();
        String detail = messageSourceHandler.getMessage(payload.getMessage(), payload.getArgs());
        ProblemDetail problem = createProblem(status, BUSINESS_TYPE, "Business rule violation", detail, request);
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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> handleNoResourceFoundException(NoResourceFoundException ex,
                                                                        HttpServletRequest request) {
        HttpStatus status = HttpStatus.NOT_FOUND;
        URI type = URI.create(String.format(HTTP_STATUS_TYPE_TEMPLATE, status.value()));
        String detail = StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : DETAIL_NOT_FOUND;
        ProblemDetail problem = createProblem(status, type, TITLE_NOT_FOUND, detail, request);
        log.debug("Static resource not found for {}", request.getRequestURI());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler({TechnicalException.class, Throwable.class})
    public ResponseEntity<ProblemDetail> handleThrowable(Throwable throwable, HttpServletRequest request) {
        String reference = UUID.randomUUID().toString();
        String detail = messageSourceHandler.getMessage(ExceptionPayloadFactory.TECHNICAL_ERROR.getMessage());
        ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_TYPE,
                "Unexpected internal error", detail, request);
        problem.setProperty(PROP_REFERENCE, reference);
        log.error("Unexpected error {} while processing {}", reference, request.getRequestURI(), throwable);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(HttpStatus status, URI type, String title, String detail,
                                        HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setInstance(requestUri(request));
        return problem;
    }

    private URI requestUri(HttpServletRequest request) {
        StringBuilder url = new StringBuilder(request.getRequestURL());
        String queryString = request.getQueryString();
        if (StringUtils.hasText(queryString)) {
            url.append('?').append(queryString);
        }
        return URI.create(url.toString());
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
}
