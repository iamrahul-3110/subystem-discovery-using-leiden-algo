package com.example.subsystemdiscovery.extraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawLinkDto(
        String from,
        String to,
        String fromPort,
        String toPort,
        String type
) {
}
