package com.example.subsystemdiscovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SubsystemDiscoveryApplication {
    public static void main(String[] args) {
        SpringApplication.run(SubsystemDiscoveryApplication.class, args);
    }
}
