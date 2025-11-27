package com.usarbcs.authen.service.resource;


import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import static com.usarbcs.core.constants.ResourcePath.AUTH;
import static com.usarbcs.core.constants.ResourcePath.V1;

@RestController
@RequestMapping(V1 + AUTH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication REST APIs")
public class AuthResource {

    private final AuthService authService;

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        @Operation(summary = "Register user", description = "Creates a new user and assigns the requested roles.")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered",
                content = @Content(mediaType = "text/plain", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", ref = "#/components/responses/ValidationProblem"),
            @ApiResponse(responseCode = "409", ref = "#/components/responses/BusinessProblem"),
            @ApiResponse(responseCode = "500", ref = "#/components/responses/InternalProblem")
        })
        public Mono<String> createPerson(@Valid @RequestBody RegisterCommand req) {
        return authService.register(req)
            .map(user -> "User created successfully with ID: " + user.getId());
    }
}
