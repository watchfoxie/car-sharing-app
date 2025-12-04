package com.usarbcs.user.userservice.handler;


import com.usarbcs.command.UserLoginCommand;
import com.usarbcs.command.UserRegisterCommand;
import com.usarbcs.user.userservice.model.User;
import com.usarbcs.user.userservice.service.UserService;
import com.usarbcs.user.userservice.utils.ValidatorHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;


@Slf4j
@Component
@RequiredArgsConstructor
public class UserHandler {

    private final UserService userService;
    private final ValidatorHandler validatorHandler;



    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(UserRegisterCommand.class)
                .doOnNext(validatorHandler::validate)
                .flatMap(userService::create)
                .doOnSuccess(userSaved -> log.info("User saved with id: {} ", userSaved.getId()))
                .doOnError(e -> log.error("Error in saveUser method", e))
                .flatMap(user -> ServerResponse.created(getToUri(user)).bodyValue(user));
    }
    public Mono<ServerResponse> loginUser(ServerRequest request) {
        return request.bodyToMono(UserLoginCommand.class)
                .flatMap(userService::login) // call login function here
                .flatMap(foundUser -> ServerResponse.ok().bodyValue("Login Successful"))
                .switchIfEmpty(ServerResponse.status(401).build());
    }

    private URI getToUri(User userSaved) {
        return UriComponentsBuilder.fromPath(("/{id}"))
                .buildAndExpand(userSaved.getId()).toUri();
    }
}
