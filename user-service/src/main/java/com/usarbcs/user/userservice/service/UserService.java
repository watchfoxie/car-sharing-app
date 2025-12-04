package com.usarbcs.user.userservice.service;

import com.usarbcs.command.UserLoginCommand;
import com.usarbcs.command.UserRegisterCommand;
import com.usarbcs.user.userservice.model.User;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface UserService {
    Mono<User> login(UserLoginCommand request);
    Mono<User> create(UserRegisterCommand userDto);
    Mono<User> retrieve(UUID userId);
}
