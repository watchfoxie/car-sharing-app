package com.usarbcs.driver.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driverServiceOpenAPI(
            @Value("${spring.application.name:driver-service}") String applicationName,
            @Value("${server.servlet.context-path:}") String contextPath) {
        final String normalizedContextPath = StringUtils.hasText(contextPath) ? contextPath : "/";
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("OpenAPI contract for the driver microservice.")
                        .version("v1"))
                .servers(List.of(new Server().url(normalizedContextPath).description("Base path")));
    }
}
