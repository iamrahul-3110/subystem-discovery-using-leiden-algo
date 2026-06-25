package com.example.subsystemdiscovery.algorithm.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable representation of the weighted dependency graph produced by
 * {@code WeightedGraphBuilder}.
 *
 * <p>This graph is the canonical input to the Leiden algorithm utility
 * ({@code LeidenAlgorithmUtil}). Nodes and edges are de-duplicated and
 * normalised before this object is created.
 */
public class WeightedGraph {
    private List<GraphNode> nodes = new ArrayList<>();
    private List<WeightedEdge> edges = new ArrayList<>();

    public WeightedGraph() {
    }

    public WeightedGraph(List<GraphNode> nodes, List<WeightedEdge> edges) {
        this.nodes = nodes;
        this.edges = edges;
    }

    public List<GraphNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<GraphNode> nodes) {
        this.nodes = nodes;
    }

    public List<WeightedEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<WeightedEdge> edges) {
        this.edges = edges;
    }
}
