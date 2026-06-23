package com.example.subsystemdiscovery.extraction.dto;

import com.example.subsystemdiscovery.leiden.model.GraphType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RawGraphDto(
        GraphType graphType,
        List<RawNodeDto> nodeDataArray,
        List<RawLinkDto> linkDataArray
) {
    public RawGraphDto withGraphType(GraphType type) {
        return new RawGraphDto(type, nodeDataArray, linkDataArray);
    }
}
