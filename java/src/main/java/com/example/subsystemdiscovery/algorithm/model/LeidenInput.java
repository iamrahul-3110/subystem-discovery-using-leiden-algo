package com.example.subsystemdiscovery.algorithm.model;

import com.example.subsystemdiscovery.algorithm.model.WeightedGraph;

public record LeidenInput(
        WeightedGraph graph,
        int runs,
        double consensusThreshold,
        double resolution
) {
}
