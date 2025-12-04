package com.usarbcs.user.userservice.config;


import com.usarbcs.command.UserLoginCommand;
import com.usarbcs.command.UserRegisterCommand;
import com.usarbcs.user.userservice.handler.UserHandler;
import com.usarbcs.user.userservice.model.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class RouterConfig {

    @Bean
    @RouterOperations({
        @RouterOperation(
            path = "/v1/auth/register",
            method = RequestMethod.POST,
            beanClass = UserHandler.class,
            beanMethod = "createUser",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE,
            operation = @Operation(
                operationId = "registerUser",
                summary = "Register a new user account",
                tags = {"Authentication"},
                requestBody = @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = UserRegisterCommand.class))),
                    responses = {
                        @ApiResponse(responseCode = "201", description = "User registered",
                            content = @Content(schema = @Schema(implementation = User.class))),
                        @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                        @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                        @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
                    }
            )
        ),
        @RouterOperation(
            path = "/v1/auth/login",
            method = RequestMethod.POST,
            beanClass = UserHandler.class,
            beanMethod = "loginUser",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE,
            operation = @Operation(
                operationId = "loginUser",
                summary = "Authenticate an existing user",
                tags = {"Authentication"},
                requestBody = @RequestBody(required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = @Schema(implementation = UserLoginCommand.class))),
                responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful",
                        content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                            schema = @Schema(implementation = String.class))),
                    @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
                    @ApiResponse(responseCode = "401", description = "Invalid credentials",
                        content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                            schema = @Schema(ref = "#/components/schemas/ProblemDetail"))),
                    @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
                    @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
                }
            )
        )
    })
    public RouterFunction<ServerResponse> route(UserHandler handler) {
        return RouterFunctions
                .route(RequestPredicates.POST("/v1/auth/login"), handler::loginUser)
                .andRoute(RequestPredicates.POST("/v1/auth/register"), handler::createUser);
    }
}
