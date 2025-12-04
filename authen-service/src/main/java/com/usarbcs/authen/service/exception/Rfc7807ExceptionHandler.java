package com.usarbcs.authen.service.exception;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayload;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.exception.MessageSourceHandler;
import com.usarbcs.core.exception.TechnicalException;
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
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RFC 7807 aligned exception handler that publishes Problem Details payloads for the authen-service REST API.
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

    private final MessageSourceHandler messageSourceHandler;

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ProblemDetail> handleWebExchangeBindException(WebExchangeBindException ex,
                                                                        ServerWebExchange exchange) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Request validation failed",
                "Payload validation failed. See the errors property for details.", exchange);
        problem.setProperty("errors", groupFieldErrors(ex));
        log.debug("Validation failed for {} with payload errors {}", exchange.getRequest().getPath(), problem.getProperties());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolationException(ConstraintViolationException ex,
                                                                            ServerWebExchange exchange) {
        ProblemDetail problem = createProblem(HttpStatus.BAD_REQUEST, VALIDATION_TYPE,
                "Constraint violation",
                "Request parameters did not satisfy validation constraints.", exchange);
        problem.setProperty("errors", groupConstraintViolations(ex));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex,
                                                                 ServerWebExchange exchange) {
        ExceptionPayload payload = ex.getPayload();
        HttpStatus status = payload.getStatus() == null ? HttpStatus.CONFLICT : payload.getStatus();
        String detail = messageSourceHandler.getMessage(payload.getMessage(), payload.getArgs());
        ProblemDetail problem = createProblem(status, BUSINESS_TYPE, "Business rule violation", detail, exchange);
        problem.setProperty("errorCode", payload.getCode());
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDetail> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex,
                                                                               ServerWebExchange exchange) {
        ExceptionPayload payload = ExceptionPayloadFactory.MISSING_REQUEST_BODY_ERROR_CODE.get();
        String detail = messageSourceHandler.getMessage(payload.getMessage());
        ProblemDetail problem = createProblem(payload.getStatus(), VALIDATION_TYPE,
                "Malformed request body", detail, exchange);
        problem.setProperty("errorCode", payload.getCode());
        return ResponseEntity.status(payload.getStatus()).body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatusException(ResponseStatusException ex,
                                                                       ServerWebExchange exchange) {
        HttpStatus status = resolveStatus(ex.getStatusCode());
        URI type = URI.create(String.format(HTTP_STATUS_TYPE_TEMPLATE, status.value()));
        String title = status.getReasonPhrase();
        String detail = ex.getReason() != null ? ex.getReason() : title;
        ProblemDetail problem = createProblem(status, type, title, detail, exchange);
        return ResponseEntity.status(ex.getStatusCode()).body(problem);
    }

    @ExceptionHandler({TechnicalException.class, Throwable.class})
    public ResponseEntity<ProblemDetail> handleThrowable(Throwable throwable, ServerWebExchange exchange) {
        String reference = UUID.randomUUID().toString();
        String detail = messageSourceHandler.getMessage(ExceptionPayloadFactory.TECHNICAL_ERROR.getMessage());
        ProblemDetail problem = createProblem(HttpStatus.INTERNAL_SERVER_ERROR, INTERNAL_TYPE,
                "Unexpected internal error", detail, exchange);
        problem.setProperty("reference", reference);
        log.error("Unexpected error {} while processing {}", reference, exchange.getRequest().getPath(), throwable);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    private ProblemDetail createProblem(HttpStatus status, URI type, String title, String detail,
                                        ServerWebExchange exchange) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(type);
        problem.setTitle(title);
        problem.setInstance(exchange.getRequest().getURI());
        return problem;
    }

    private Map<String, List<String>> groupFieldErrors(WebExchangeBindException ex) {
        return ex.getFieldErrors().stream()
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
