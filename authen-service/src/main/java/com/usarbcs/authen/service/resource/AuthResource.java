package com.usarbcs.authen.service.resource;


import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static com.usarbcs.core.constants.ResourcePath.AUTH;
import static com.usarbcs.core.constants.ResourcePath.V1;

@RestController
@RequestMapping(V1 + AUTH)
@RequiredArgsConstructor
public class AuthResource {

    private final AuthService authService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<String> createPerson(@RequestBody RegisterCommand req) {
        return authService.register(req)
                .map(userId -> "User created successfully with ID: " + userId);
    }
}
