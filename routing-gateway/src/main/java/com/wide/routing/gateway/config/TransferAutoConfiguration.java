package com.wide.routing.gateway.config;

import com.wide.routing.gateway.filter.DynamicLoadBalancerClientFilter;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.gateway.config.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(GatewayLoadBalancerClientAutoConfiguration.class)
@EnableConfigurationProperties(LoadBalancerProperties.class)
public class TransferAutoConfiguration {

    @Bean
    public DynamicLoadBalancerClientFilter dynamicLoadBalancerClientFilter(LoadBalancerProperties properties) {
        return new DynamicLoadBalancerClientFilter(properties);
    }

}
