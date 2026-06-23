package com.example.subsystemdiscovery.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawFieldDto(
        String key,
        String name,
        String type,
        Boolean colorFlag
) {
}
