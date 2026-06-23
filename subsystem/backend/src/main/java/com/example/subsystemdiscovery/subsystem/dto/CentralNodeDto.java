package com.example.subsystemdiscovery.subsystem.dto;

public record CentralNodeDto(
        Long id,
        String name,
        String qualifiedName,
        String type,
        double score
) {
}
