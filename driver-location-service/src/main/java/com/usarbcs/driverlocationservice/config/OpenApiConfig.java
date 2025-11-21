package com.usarbcs.driverlocationservice.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI driverLocationOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8083").description("Local"))
                .info(new Info()
                        .title("Driver Location Service API")
                        .description("Operations for managing driver geo locations")
                        .version("v1"))
                .externalDocs(new ExternalDocumentation()
                        .description("Swagger UI")
                        .url("http://localhost:8083/swagger-ui.html"));
    }
}
