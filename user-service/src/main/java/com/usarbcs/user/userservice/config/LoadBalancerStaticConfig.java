package com.usarbcs.user.userservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.client.loadbalancer.DeferringLoadBalancerInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRestClientBuilderBeanPostProcessor;
import org.springframework.cloud.client.loadbalancer.reactive.DeferringLoadBalancerExchangeFilterFunction;
import org.springframework.cloud.client.loadbalancer.reactive.LoadBalancerWebClientBuilderBeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Re-registers Spring Cloud LoadBalancer infrastructure beans using static definitions so that
 * BeanPostProcessorChecker warnings disappear.
 */
@Configuration(proxyBeanMethods = false)
public class LoadBalancerStaticConfig implements BeanDefinitionRegistryPostProcessor, Ordered {

    private static final String DEFERRING_INTERCEPTOR = "deferringLoadBalancerInterceptor";
    private static final String LB_REST_CLIENT_BPP = "lbRestClientPostProcessor";
    private static final String REACTOR_DEFERRING_FILTER = "reactorDeferringLoadBalancerExchangeFilterFunction";
    private static final String LB_WEBCLIENT_BPP = "loadBalancerWebClientBuilderBeanPostProcessor";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        replaceBeanDefinition(registry, DEFERRING_INTERCEPTOR, DeferringLoadBalancerInterceptor.class);
        replaceBeanDefinition(registry, LB_REST_CLIENT_BPP, LoadBalancerRestClientBuilderBeanPostProcessor.class);
        replaceBeanDefinition(registry, REACTOR_DEFERRING_FILTER, DeferringLoadBalancerExchangeFilterFunction.class);
        replaceBeanDefinition(registry, LB_WEBCLIENT_BPP, LoadBalancerWebClientBuilderBeanPostProcessor.class);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }

    private void replaceBeanDefinition(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
        RootBeanDefinition definition = new RootBeanDefinition(beanClass);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(beanName, definition);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
