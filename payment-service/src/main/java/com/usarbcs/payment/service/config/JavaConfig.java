package com.usarbcs.payment.service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;

@Configuration
public class JavaConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> Optional.of("PAYMENT_SERVICE");
    }
}
