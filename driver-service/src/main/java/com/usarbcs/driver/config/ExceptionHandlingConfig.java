package com.usarbcs.driver.config;

import com.usarbcs.core.exception.MessageSourceHandler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Exposes shared exception handling infrastructure beans sourced from the core-comm module.
 */
@Configuration
public class ExceptionHandlingConfig {

    @Bean
    public MessageSourceHandler messageSourceHandler(MessageSource messageSource) {
        return new MessageSourceHandler(messageSource);
    }
}
