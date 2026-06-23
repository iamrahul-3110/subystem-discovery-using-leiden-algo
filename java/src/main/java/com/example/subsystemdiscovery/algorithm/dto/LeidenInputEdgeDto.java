package com.example.subsystemdiscovery.algorithm.dto;

public record LeidenInputEdgeDto(
        Long source,
        Long target,
        double weight
) {
}
