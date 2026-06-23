package com.example.subsystemdiscovery.leiden.model;

import com.example.subsystemdiscovery.leiden.model.WeightedGraph;

public record LeidenInput(
        WeightedGraph graph,
        int runs,
        double consensusThreshold,
        double resolution
) {
}
