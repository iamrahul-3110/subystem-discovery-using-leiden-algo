package com.example.subsystemdiscovery.algorithm.dto;

import java.util.List;

public record LeidenInputDto(
        int nodeCount,
        int edgeCount,
        List<Long> nodes,
        List<LeidenInputEdgeDto> edges
) {
}
