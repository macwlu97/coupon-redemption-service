package com.redemption.gateway;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Single entry point for the system.
 * Routes traffic to appropriate microservices using Eureka service names.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @PreDestroy
    public void onDestroy() {
        System.out.println("Application shutting down.");
    }
}
