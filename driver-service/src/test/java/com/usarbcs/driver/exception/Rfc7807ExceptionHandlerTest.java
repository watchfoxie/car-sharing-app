package com.usarbcs.driver.exception;

import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayload;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.exception.MessageSourceHandler;
import com.usarbcs.core.exception.TechnicalException;
import com.usarbcs.driver.command.DriverCommand;
import com.usarbcs.driver.controller.DriverController;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
    void handleMethodArgumentNotValidExceptionShouldGroupFieldErrors() throws NoSuchMethodException {
        DriverCommand command = new DriverCommand();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(command, "driverCommand");
        bindingResult.addError(new FieldError("driverCommand", "firstName", "must not be blank"));
        bindingResult.addError(new FieldError("driverCommand", "firstName", "must contain only letters"));

        Method method = DriverController.class.getDeclaredMethod("create", DriverCommand.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleMethodArgumentNotValidException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/request-validation"));
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) body.getProperties().get("errors");
        assertThat(errors.get("firstName")).containsExactly("must not be blank", "must contain only letters");
    }

    @Test
    void handleConstraintViolationExceptionShouldAggregateMessages() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                violation("driverCommand.firstName", "invalid"),
                violation("driverCommand.lastName", "required")
        ));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(exception, request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertThat(errors)
                .containsEntry("driverCommand.firstName", List.of("invalid"))
                .containsEntry("driverCommand.lastName", List.of("required"));
    }

    @Test
    void handleBusinessExceptionShouldUsePayloadStatusAndCode() {
        ExceptionPayload payload = ExceptionPayload.builder()
                .code(42)
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .message("business.error")
                .build();
        when(messageSourceHandler.getMessage(eq("business.error"), ArgumentMatchers.<Object[]>any()))
                .thenReturn("Business message");

        BusinessException exception = new BusinessException(payload, "arg1");
        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(exception, request());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body.getProperties()).containsEntry("errorCode", 42);
        assertThat(body.getDetail()).isEqualTo("Business message");
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldExposeErrorCode() {
        when(messageSourceHandler.getMessage("request.missing.body")).thenReturn("Missing body");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("broken", (Throwable) null, (HttpInputMessage) null);

        ResponseEntity<ProblemDetail> response = handler.handleHttpMessageNotReadableException(exception, request());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body.getProperties()).containsEntry(
                "errorCode", ExceptionPayloadFactory.MISSING_REQUEST_BODY_ERROR_CODE.get().getCode());
        assertThat(body.getDetail()).isEqualTo("Missing body");
    }

    @Test
    void handleResponseStatusExceptionShouldDeriveProblemType() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

        ResponseEntity<ProblemDetail> response = handler.handleResponseStatusException(exception, request());

        ProblemDetail body = response.getBody();
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/http-404"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.getDetail()).isEqualTo("Not found");
    }

    @Test
    void handleThrowableShouldAttachReferenceAndUseDefaultMessage() {
        when(messageSourceHandler.getMessage("technical.error")).thenReturn("Technical issue");

        ResponseEntity<ProblemDetail> response = handler.handleThrowable(new TechnicalException("boom"), request());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getProperties()).containsKey("reference");
        assertThat(body.getDetail()).isEqualTo("Technical issue");
        verify(messageSourceHandler).getMessage("technical.error");
    }

    @Test
    void handleNoResourceFoundExceptionShouldReturnNotFoundProblem() {
        NoResourceFoundException exception = new NoResourceFoundException(HttpMethod.GET, "/");

        ResponseEntity<ProblemDetail> response = handler.handleNoResourceFoundException(exception, request());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.getTitle()).isEqualTo("Resource not found");
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/http-404"));
        assertThat(body.getDetail()).contains("No static resource");
    }

    private static MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/drivers");
        request.setServerName("driver-service");
        request.setServerPort(8087);
        return request;
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
            public <U> U unwrap(Class<U> type) {
                throw new UnsupportedOperationException("Not implemented");
            }
        };
    }
}
