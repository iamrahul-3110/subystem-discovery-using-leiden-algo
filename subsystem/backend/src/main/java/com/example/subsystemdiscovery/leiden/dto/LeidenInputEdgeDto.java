package com.example.subsystemdiscovery.leiden.dto;

public record LeidenInputEdgeDto(
        Long source,
        Long target,
        double weight
) {
}
