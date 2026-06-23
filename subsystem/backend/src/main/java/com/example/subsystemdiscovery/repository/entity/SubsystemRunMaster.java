package com.example.subsystemdiscovery.repository.entity;

import java.time.LocalDateTime;

/**
 * Represents one row in the simplified {@code tb_gi_subsystems_history} table.
 */
public class SubsystemRunMaster {
    private Long discoveryRunId;
    private String analysisTime;
    private Integer runSequence;
    private Integer runs;
    private Double consensusThreshold;
    private Double resolution;
    private String discoveryResult; // JSON string
    private Integer totalSubsystems;
    private Double avgStabilityScore;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private byte[] createdByEnvelope;
    private byte[] createdByHash;

    public Long getDiscoveryRunId() {
        return discoveryRunId;
    }

    public void setDiscoveryRunId(Long discoveryRunId) {
        this.discoveryRunId = discoveryRunId;
    }

    public String getAnalysisTime() {
        return analysisTime;
    }

    public void setAnalysisTime(String analysisTime) {
        this.analysisTime = analysisTime;
    }

    public Integer getRunSequence() {
        return runSequence;
    }

    public void setRunSequence(Integer runSequence) {
        this.runSequence = runSequence;
    }

    public Integer getRuns() {
        return runs;
    }

    public void setRuns(Integer runs) {
        this.runs = runs;
    }

    public Double getConsensusThreshold() {
        return consensusThreshold;
    }

    public void setConsensusThreshold(Double consensusThreshold) {
        this.consensusThreshold = consensusThreshold;
    }

    public Double getResolution() {
        return resolution;
    }

    public void setResolution(Double resolution) {
        this.resolution = resolution;
    }

    public String getDiscoveryResult() {
        return discoveryResult;
    }

    public void setDiscoveryResult(String discoveryResult) {
        this.discoveryResult = discoveryResult;
    }

    public Integer getTotalSubsystems() {
        return totalSubsystems;
    }

    public void setTotalSubsystems(Integer totalSubsystems) {
        this.totalSubsystems = totalSubsystems;
    }

    public Double getAvgStabilityScore() {
        return avgStabilityScore;
    }

    public void setAvgStabilityScore(Double avgStabilityScore) {
        this.avgStabilityScore = avgStabilityScore;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public byte[] getCreatedByEnvelope() {
        return createdByEnvelope;
    }

    public void setCreatedByEnvelope(byte[] createdByEnvelope) {
        this.createdByEnvelope = createdByEnvelope;
    }

    public byte[] getCreatedByHash() {
        return createdByHash;
    }

    public void setCreatedByHash(byte[] createdByHash) {
        this.createdByHash = createdByHash;
    }
}
