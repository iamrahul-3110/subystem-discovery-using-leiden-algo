package com.example.subsystemdiscovery.subsystem.dto;

public record SummaryDto(
        int totalNodes,
        int totalEdges,
        int subsystemCount,
        double averageStability
) {
}
