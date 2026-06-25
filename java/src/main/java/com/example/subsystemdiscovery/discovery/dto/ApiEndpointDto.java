package com.example.subsystemdiscovery.discovery.dto;

public record ApiEndpointDto(
        Long id,
        String method,
        String path
) {
}
