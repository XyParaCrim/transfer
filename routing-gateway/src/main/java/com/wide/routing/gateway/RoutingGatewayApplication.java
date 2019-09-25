package com.wide.routing.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class RoutingGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoutingGatewayApplication.class, args);
    }
}
