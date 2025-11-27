package com.usarbcs.user.userservice.service;

import com.usarbcs.command.UserLoginCommand;
import com.usarbcs.command.UserRegisterCommand;
import com.usarbcs.user.userservice.model.User;
import com.usarbcs.user.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.UUID;




@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService{


    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    public Mono<User> create(UserRegisterCommand request) {
        return Mono.just(request)
                .doOnNext(UserRegisterCommand::validate)
                .flatMap(cmd -> {
                    User user = User.create(cmd);
                    user.setPassword(passwordEncoder.encode(user.getPassword()));
                    return userRepository.save(user);
            });
    }
    @Override
    public Mono<User> login(UserLoginCommand request) {
        return Mono.just(request)
                .doOnNext(UserLoginCommand::validate)
                .flatMap(cmd -> findByEmail(cmd.getEmail())
                        .flatMap(foundUser -> passwordEncoder.matches(cmd.getPassword(), foundUser.getPassword())
                                ? Mono.just(foundUser)
                                : Mono.error(unauthorizedException())));
    }

    private Mono<User> findByEmail(String email) {
        return userRepository.findActiveByEmail(email)
                .switchIfEmpty(Mono.error(unauthorizedException()));
    }

    private ResponseStatusException unauthorizedException() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    @Override
    public Mono<User> retrieve(UUID userId) {
        return userRepository.findById(userId);
    }
}
