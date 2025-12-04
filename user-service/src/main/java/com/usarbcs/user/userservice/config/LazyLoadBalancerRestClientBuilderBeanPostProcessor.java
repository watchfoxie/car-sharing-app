package com.usarbcs.user.userservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.ApplicationContext;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

/**
 * Registers the load-balanced interceptor lazily to avoid eager bean creation warnings.
 */
public class LazyLoadBalancerRestClientBuilderBeanPostProcessor implements BeanPostProcessor {

    private final ApplicationContext applicationContext;

    public LazyLoadBalancerRestClientBuilderBeanPostProcessor(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof RestClient.Builder builder
                && applicationContext.findAnnotationOnBean(beanName, LoadBalanced.class) != null) {
            try {
                ClientHttpRequestInterceptor interceptor = applicationContext.getBean(
                        "deferringLoadBalancerInterceptor", ClientHttpRequestInterceptor.class);
                builder.requestInterceptor(interceptor);
            } catch (NoSuchBeanDefinitionException ignored) {
                // Load balancer interceptor not available in this context.
            }
        }
        return bean;
    }
}
