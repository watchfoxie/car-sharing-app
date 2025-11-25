package com.usarbcs.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.cloud.gateway.ratelimit")
public class RateLimitProperties {

    private int capacity = 60;
    private int refillTokens = 60;
    private Duration refillPeriod = Duration.ofMinutes(1);
    private String headerName = "X-Client-Id";
}
