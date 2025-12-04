package com.usarbcs.user.userservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Registers the reactive load-balancer filter lazily to avoid early bean instantiation warnings.
 */
public class LazyLoadBalancerWebClientBuilderBeanPostProcessor implements BeanPostProcessor {

    private final ApplicationContext applicationContext;

    public LazyLoadBalancerWebClientBuilderBeanPostProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof WebClient.Builder builder
                && applicationContext.findAnnotationOnBean(beanName, LoadBalanced.class) != null) {
            try {
                ExchangeFilterFunction filter = applicationContext.getBean(
                        "reactorDeferringLoadBalancerExchangeFilterFunction", ExchangeFilterFunction.class);
                builder.filter(filter);
            } catch (NoSuchBeanDefinitionException ignored) {
                // Reactive load balancer filter not available in this context.
            }
        }
        return bean;
    }
}
