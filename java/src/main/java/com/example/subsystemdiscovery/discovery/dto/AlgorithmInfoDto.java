package com.example.subsystemdiscovery.discovery.dto;

public record AlgorithmInfoDto(
        String name,
        int runs,
        double consensusThreshold,
        double resolution,
        String weightingVersion
) {
}
