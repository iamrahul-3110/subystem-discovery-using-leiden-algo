package com.example.subsystemdiscovery.subsystem.dto;

public record AlgorithmInfoDto(
        String name,
        int runs,
        double consensusThreshold,
        double resolution,
        String weightingVersion
) {
}
