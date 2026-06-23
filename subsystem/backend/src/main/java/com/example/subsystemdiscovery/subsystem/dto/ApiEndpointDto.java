package com.example.subsystemdiscovery.subsystem.dto;

public record ApiEndpointDto(
        Long id,
        String method,
        String path
) {
}
