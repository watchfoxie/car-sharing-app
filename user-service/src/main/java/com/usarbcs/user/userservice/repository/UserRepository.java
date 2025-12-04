package com.usarbcs.user.userservice.repository;


import com.usarbcs.user.userservice.model.User;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;


@Repository
public interface UserRepository extends ReactiveCrudRepository<User, UUID> {

    @Query("SELECT * FROM user_account WHERE LOWER(email) = LOWER($1) AND deleted IS FALSE")
    Mono<User> findActiveByEmail(String email);
}
