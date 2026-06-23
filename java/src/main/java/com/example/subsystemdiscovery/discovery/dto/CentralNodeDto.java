package com.example.subsystemdiscovery.discovery.dto;

public record CentralNodeDto(
        Long id,
        String name,
        String qualifiedName,
        String type,
        double score
) {
}
