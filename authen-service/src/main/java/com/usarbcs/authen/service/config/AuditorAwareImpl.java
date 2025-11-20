package com.usarbcs.authen.service.config;

import org.springframework.data.domain.ReactiveAuditorAware;
import reactor.core.publisher.Mono;

public class AuditorAwareImpl implements ReactiveAuditorAware<String> {

    @Override
    public Mono<String> getCurrentAuditor() {
        return Mono.just("CARSHARING");
    }

}
