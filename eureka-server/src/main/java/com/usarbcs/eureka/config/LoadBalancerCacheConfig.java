package com.usarbcs.eureka.config;

import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.loadbalancer.cache.CaffeineBasedLoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheManager;
import org.springframework.cloud.loadbalancer.cache.LoadBalancerCacheProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Configuration
@EnableCaching
@EnableConfigurationProperties(LoadBalancerCacheProperties.class)
public class LoadBalancerCacheConfig {

    /**
     * Register a {@link LoadBalancerCacheManager} backed by Caffeine so the
     * Spring Cloud LoadBalancer stops falling back to a development cache.
     */
    @Bean
    public LoadBalancerCacheManager caffeineLoadBalancerCacheManager(LoadBalancerCacheProperties properties) {
        // Provide explicit defaults when the properties are not configured.
        if (properties.getTtl() == null) {
            properties.setTtl(Duration.ofMinutes(5));
        }
        if (properties.getCapacity() <= 0) {
            properties.setCapacity(500);
        }
        LoadBalancerCacheProperties.Caffeine caffeine = properties.getCaffeine();
        if (caffeine == null) {
            caffeine = new LoadBalancerCacheProperties.Caffeine();
            properties.setCaffeine(caffeine);
        }
        if (!StringUtils.hasText(caffeine.getSpec())) {
            caffeine.setSpec("initialCapacity=50,maximumSize=500,expireAfterWrite=5m");
        }
        return new CaffeineBasedLoadBalancerCacheManager("springCloudLoadBalancer", properties);
    }
}
