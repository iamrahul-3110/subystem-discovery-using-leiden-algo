package com.example.subsystemdiscovery.algorithm.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class LeidenClusteringResult {
    private Map<Long, String> clusters = new LinkedHashMap<>();
    private Map<String, Double> stabilityScores = new LinkedHashMap<>();

    public LeidenClusteringResult() {
    }

    public LeidenClusteringResult(Map<Long, String> clusters, Map<String, Double> stabilityScores) {
        this.clusters = clusters;
        this.stabilityScores = stabilityScores;
    }

    public Map<Long, String> getClusters() {
        return clusters;
    }

    public void setClusters(Map<Long, String> clusters) {
        this.clusters = clusters;
    }

    public Map<String, Double> getStabilityScores() {
        return stabilityScores;
    }

    public void setStabilityScores(Map<String, Double> stabilityScores) {
        this.stabilityScores = stabilityScores;
    }
}
