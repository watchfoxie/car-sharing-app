package com.usarbcs.rating.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ratingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Rating Service API")
                        .version("v1")
                        .description("Operations for persisting and aggregating driver ratings in the car sharing platform.")
                        .contact(new Contact().name("Car Sharing Platform").email("support@car-sharing.example"))
                        .license(new License().name("Apache 2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8086/rating-service").description("Local environment")
                ));
    }

    @Bean
    public GroupedOpenApi ratingServiceGroup() {
        return GroupedOpenApi.builder()
                .group("rating-service")
                .packagesToScan("com.usarbcs.rating.controller")
                .pathsToMatch("/v1/**")
                .build();
    }
}
