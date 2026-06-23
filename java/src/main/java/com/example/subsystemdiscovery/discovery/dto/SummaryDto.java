package com.example.subsystemdiscovery.discovery.dto;

public record SummaryDto(
        int totalNodes,
        int totalEdges,
        int subsystemCount,
        double averageStability
) {
}
