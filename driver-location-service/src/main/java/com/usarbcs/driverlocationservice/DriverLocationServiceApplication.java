package com.usarbcs.driverlocationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class DriverLocationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverLocationServiceApplication.class, args);
    }
}
