package com.usarbcs.authen.service.repository;

import com.usarbcs.authen.service.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Repository
public interface AuthRepository extends ReactiveCrudRepository<User, UUID> {

    @Query("SELECT * FROM auth_user WHERE LOWER(email) = LOWER($1) AND deleted IS FALSE")
    Mono<User> findActiveByEmail(String email);
}
