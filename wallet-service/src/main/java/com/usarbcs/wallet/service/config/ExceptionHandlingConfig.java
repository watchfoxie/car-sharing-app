package com.usarbcs.wallet.service.config;

import com.usarbcs.core.exception.MessageSourceHandler;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the MessageSource-backed helper consumed by the RFC 7807 exception handler.
 */
@Configuration
public class ExceptionHandlingConfig {

    @Bean
    public MessageSourceHandler messageSourceHandler(MessageSource messageSource) {
        return new MessageSourceHandler(messageSource);
    }
}
