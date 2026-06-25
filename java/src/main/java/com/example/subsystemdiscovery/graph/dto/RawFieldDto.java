package com.example.subsystemdiscovery.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawFieldDto(
        String key,
        String name,
        String type,
        Boolean colorFlag
) {
}
