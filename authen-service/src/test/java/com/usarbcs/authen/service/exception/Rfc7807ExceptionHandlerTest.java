package com.usarbcs.authen.service.exception;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.resource.AuthResource;
import com.usarbcs.core.exception.BusinessException;
import com.usarbcs.core.exception.ExceptionPayload;
import com.usarbcs.core.exception.ExceptionPayloadFactory;
import com.usarbcs.core.exception.MessageSourceHandler;
import com.usarbcs.core.exception.TechnicalException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

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
    void handleWebExchangeBindExceptionShouldGroupFieldErrors() throws NoSuchMethodException {
        RegisterCommand command = new RegisterCommand();
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(command, "registerCommand");
        bindingResult.addError(new FieldError("registerCommand", "email", "must not be blank"));
        bindingResult.addError(new FieldError("registerCommand", "email", "must contain @"));

        Method method = AuthResource.class.getDeclaredMethod("createPerson", RegisterCommand.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        WebExchangeBindException exception = new WebExchangeBindException(parameter, bindingResult);

        ResponseEntity<ProblemDetail> response = handler.handleWebExchangeBindException(exception, exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/request-validation"));
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) body.getProperties().get("errors");
        assertThat(errors.get("email")).containsExactly("must not be blank", "must contain @");
    }

    @Test
    void handleConstraintViolationExceptionShouldAggregateMessages() {
        ConstraintViolationException exception = new ConstraintViolationException(Set.of(
                violation("command.email", "invalid email"),
                violation("command.phoneNumber", "invalid phone")
        ));

        ResponseEntity<ProblemDetail> response = handler.handleConstraintViolationException(exception, exchange());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        @SuppressWarnings("unchecked")
        Map<String, List<String>> errors = (Map<String, List<String>>) response.getBody().getProperties().get("errors");
        assertThat(errors)
                .containsEntry("command.email", List.of("invalid email"))
                .containsEntry("command.phoneNumber", List.of("invalid phone"));
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
        ResponseEntity<ProblemDetail> response = handler.handleBusinessException(exception, exchange());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(body.getProperties()).containsEntry("errorCode", 42);
        assertThat(body.getDetail()).isEqualTo("Business message");
    }

    @Test
    void handleHttpMessageNotReadableExceptionShouldExposeErrorCode() {
        when(messageSourceHandler.getMessage("request.missing.body")).thenReturn("Missing body");
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("broken payload", (Throwable) null, (HttpInputMessage) null);

        ResponseEntity<ProblemDetail> response = handler.handleHttpMessageNotReadableException(exception, exchange());

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

        ResponseEntity<ProblemDetail> response = handler.handleResponseStatusException(exception, exchange());

        ProblemDetail body = response.getBody();
        assertThat(body.getType()).isEqualTo(URI.create("https://car-sharing.app/problems/http-404"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(body.getDetail()).isEqualTo("Not found");
    }

    @Test
    void handleThrowableShouldAttachReferenceAndUseDefaultMessage() {
        when(messageSourceHandler.getMessage("technical.error")).thenReturn("Technical issue");

        ResponseEntity<ProblemDetail> response = handler.handleThrowable(new TechnicalException("boom"), exchange());

        ProblemDetail body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(body.getProperties()).containsKey("reference");
        assertThat(body.getDetail()).isEqualTo("Technical issue");
        verify(messageSourceHandler).getMessage("technical.error");
    }

    private static ServerWebExchange exchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.post("/v1/auth").build());
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
