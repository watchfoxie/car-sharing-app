package com.usarbcs.driver.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.usarbcs.core.util.GenericRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Configuration
public class JavaConfig {



    @Bean
    @LoadBalanced
    public RestTemplate restTemplate(RestTemplateBuilder builder){
        return builder.build();
    }

    @Bean
    public GenericRestTemplate genericRestTemplate(RestTemplate restTemplate, ObjectMapper objectMapper){
        return new GenericRestTemplate(restTemplate, objectMapper);
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer(){
        return builder -> builder
                .modules(new JavaTimeModule())
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.of("USARBCS");
    }
}
