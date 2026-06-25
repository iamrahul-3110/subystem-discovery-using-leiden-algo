package com.example.subsystemdiscovery.graph.dto;

import com.example.subsystemdiscovery.algorithm.model.GraphType;
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
