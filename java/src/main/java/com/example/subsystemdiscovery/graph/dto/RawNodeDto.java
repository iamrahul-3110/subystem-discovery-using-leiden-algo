package com.example.subsystemdiscovery.graph.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawNodeDto(
        String key,
        String text,
        String name,
        String type,
        String packageName,
        String group,
        @JsonProperty("isGroup") Boolean isGroup,
        List<RawFieldDto> fields
) {
}
