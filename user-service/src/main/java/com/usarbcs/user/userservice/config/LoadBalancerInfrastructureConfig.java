package com.usarbcs.user.userservice.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Overrides load-balancer BeanPostProcessors with lazy equivalents to prevent BeanPostProcessorChecker warnings.
 */
@Configuration(proxyBeanMethods = false)
public class LoadBalancerInfrastructureConfig implements BeanDefinitionRegistryPostProcessor, Ordered {

    private static final String LB_REST_CLIENT_BPP = "lbRestClientPostProcessor";
    private static final String LB_WEBCLIENT_BPP = "loadBalancerWebClientBuilderBeanPostProcessor";

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        replaceDefinition(registry, LB_REST_CLIENT_BPP, LazyLoadBalancerRestClientBuilderBeanPostProcessor.class);
        replaceDefinition(registry, LB_WEBCLIENT_BPP, LazyLoadBalancerWebClientBuilderBeanPostProcessor.class);
    }

    private void replaceDefinition(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass) {
        if (registry.containsBeanDefinition(beanName)) {
            registry.removeBeanDefinition(beanName);
        }
        RootBeanDefinition definition = new RootBeanDefinition(beanClass);
        definition.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_CONSTRUCTOR);
        definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        registry.registerBeanDefinition(beanName, definition);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        // no-op
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
