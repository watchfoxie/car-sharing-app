package com.usarbcs.rating.config;

import com.usarbcs.core.exception.MessageSourceHandler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers shared exception-handling infrastructure beans that live in the core-comm module.
 */
@Configuration
public class ExceptionHandlingConfig {

    @Bean
    public MessageSourceHandler messageSourceHandler(MessageSource messageSource) {
        return new MessageSourceHandler(messageSource);
    }
}
