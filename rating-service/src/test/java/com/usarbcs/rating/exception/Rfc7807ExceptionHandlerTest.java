package com.usarbcs.rating.exception;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayload;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.exception.MessageSourceHandler;
import com.usarbcs.core.exception.TechnicalException;
import com.usarbcs.rating.command.RatingCommand;
import com.usarbcs.rating.controller.RatingController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.metadata.ConstraintDescriptor;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Rfc7807ExceptionHandlerTest {

    @Mock
    private MessageSourceHandler messageSourceHandler;

    private Rfc7807ExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new Rfc7807ExceptionHandler(messageSourceHandler);
    }

    @Test
    void handleMethodArgumentNotValidGroupsFieldErrors() throws NoSuchMethodException {
        RatingCommand command = new RatingCommand();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(command, "ratingCommand");
        bindingResult.addError(new FieldError("ratingCommand", "driverId", "must not be blank"));
        bindingResult.addError(new FieldError("ratingCommand", "driverId", "size must be between 1 and 64"));

        Method method = RatingController.class.getDeclaredMethod("create", RatingCommand.class);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(
                new org.springframework.core.MethodParameter(method, 0), bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValid(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertThat(errors.get("driverId")).containsExactly("must not be blank", "size must be between 1 and 64");
    }

    @Test
    void handleConstraintViolationAggregatesMessages() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                violation("command.driverId", "must not be blank"),
                violation("command.ratingScore", "must be between 1 and 5")
        ));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertThat(errors)
                .containsEntry("command.driverId", List.of("must not be blank"))
                .containsEntry("command.ratingScore", List.of("must be between 1 and 5"));
    }

    @Test
    void handleBusinessExceptionUsesPayloadMetadata() {
        ExceptionPayload payload = ExceptionPayload.builder()
                .code(42)
                .status(HttpStatus.CONFLICT)
                .message("business.error")
                .build();
        when(messageSourceHandler.getMessage(eq("business.error"), ArgumentMatchers.<Object[]>any()))
                .thenReturn("Rating already exists");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(new BusinessException(payload, "drv"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        ProblemDetail body = response.getBody();
        assertThat(body.getDetail()).isEqualTo("Rating already exists");
        assertThat(body.getProperties()).containsEntry("errorCode", 42);
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/business-rule-violation"));
    }

    @Test
    void handleBusinessExceptionMapsNotFoundPayloads() {
        ExceptionPayload payload = ExceptionPayload.builder()
                .code(10)
                .status(HttpStatus.NOT_FOUND)
                .message("rating.not.found")
                .build();
        when(messageSourceHandler.getMessage("rating.not.found")).thenReturn("Rating not found");

        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(new BusinessException(payload), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getType())
                .isEqualTo(URI.create("https://car-sharing.app/problems/resource-not-found"));
        assertThat(response.getBody().getTitle()).isEqualTo("Resource not found");
    }

    @Test
    void handleHttpMessageNotReadableAddsErrorCode() {
        when(messageSourceHandler.getMessage("request.missing.body")).thenReturn("Missing body");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
            "broken", (Throwable) null, (HttpInputMessage) null);

        ResponseEntity<ProblemDetail> response = handler.handleHttpMessageNotReadableException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getProperties())
                .containsEntry("errorCode", ExceptionPayloadFactory.MISSING_REQUEST_BODY_ERROR_CODE.get().getCode());
    }

    @Test
    void handleResponseStatusExceptionBuildsHttpSpecificType() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Not acceptable");

        ResponseEntity<ProblemDetail> response = handler.handleResponseStatusException(exception, request());

        assertThat(response.getBody().getType())
                .isEqualTo(URI.create("https://car-sharing.app/problems/http-406"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
    }

    @Test
    void handleThrowableAttachesReference() {
        when(messageSourceHandler.getMessage("technical.error")).thenReturn("Technical issue");

        ResponseEntity<ProblemDetail> response = handler.handleThrowable(new TechnicalException("boom"), request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getProperties()).containsKey("reference");
        verify(messageSourceHandler).getMessage("technical.error");
    }

    @Test
    void handleMissingResourceReturnsNotFoundProblem() {
        NoHandlerFoundException exception = new NoHandlerFoundException("GET", "/rating-service/missing", HttpHeaders.EMPTY);

        ResponseEntity<ProblemDetail> response = handler.handleMissingResource(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getType())
                .isEqualTo(URI.create("https://car-sharing.app/problems/resource-not-found"));
    }

    private static HttpServletRequest request() {
        return new MockHttpServletRequest("POST", "/rating-service/v1/ratings");
    }

    private static ConstraintViolation<?> violation(String property, String message) {
        return new ConstraintViolation<>() {
            @Override
            public String getMessage() {
                return message;
            }

            @Override
            public String getMessageTemplate() {
                return message;
            }

            @Override
            public Object getRootBean() {
                return null;
            }

            @Override
            public Class<Object> getRootBeanClass() {
                return Object.class;
            }

            @Override
            public Object getLeafBean() {
                return null;
            }

            @Override
            public Object[] getExecutableParameters() {
                return new Object[0];
            }

            @Override
            public Object getExecutableReturnValue() {
                return null;
            }

            @Override
            public Path getPropertyPath() {
                return PathImpl.createPathFromString(property);
            }

            @Override
            public Object getInvalidValue() {
                return null;
            }

            @Override
            public ConstraintDescriptor<?> getConstraintDescriptor() {
                return null;
            }

            @Override
            public <T> T unwrap(Class<T> type) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }
}
