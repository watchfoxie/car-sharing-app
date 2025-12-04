package com.usarbcs.driverlocationservice.config;

import com.usarbcs.core.exception.MessageSourceHandler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the shared MessageSource-backed helper used by the RFC 7807 exception handler.
 */
@Configuration
public class ExceptionHandlingConfig {

    @Bean
    public MessageSourceHandler messageSourceHandler(MessageSource messageSource) {
        return new MessageSourceHandler(messageSource);
    }
}
