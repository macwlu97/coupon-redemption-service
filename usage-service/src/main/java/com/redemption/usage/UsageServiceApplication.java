package com.redemption.usage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EnableDiscoveryClient
@EntityScan("com.redemption.usage.domain.model")
public class UsageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UsageServiceApplication.class, args);
    }
}
