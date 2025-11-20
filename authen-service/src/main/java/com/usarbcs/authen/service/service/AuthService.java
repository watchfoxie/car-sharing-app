package com.usarbcs.authen.service.service;

import com.usarbcs.authen.service.command.RegisterCommand;
import com.usarbcs.authen.service.model.User;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<User> register(RegisterCommand request);
}
