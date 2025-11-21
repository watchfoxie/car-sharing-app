package com.usarbcs.wallet.service.config;

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
    public OpenAPI walletServiceOpenAPI(
            @Value("${spring.application.name:wallet}") String applicationName,
            @Value("${server.servlet.context-path:}") String contextPath) {
        final String normalizedContextPath = StringUtils.hasText(contextPath) ? contextPath : "/";
        return new OpenAPI()
                .info(new Info()
                        .title(applicationName + " API")
                        .description("OpenAPI contract for the wallet microservice.")
                        .version("v1"))
                .servers(List.of(new Server().url(normalizedContextPath).description("Base path")));
    }
}
