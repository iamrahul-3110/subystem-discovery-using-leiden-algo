package com.example.subsystemdiscovery.repository;

import com.example.subsystemdiscovery.repository.entity.SubsystemRunMaster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SubsystemHistoryMapper {

    /**
     * Inserts a new discovery run record.
     * The auto-generated {@code discoveryRunId} is written back into the entity.
     */
    void insertDiscovery(SubsystemRunMaster master);

    /**
     * Retrieves the serialised discovery result JSON for a given run.
     */
    String selectHistoryResult(@Param("discoveryRunId") Long discoveryRunId);

    /**
     * Finds the most recent run that matches the given algorithm configuration.
     */
    SubsystemRunMaster selectMasterByConfig(
            @Param("analysisTime")       String analysisTime,
            @Param("runs")               int    runs,
            @Param("consensusThreshold") double consensusThreshold,
            @Param("resolution")         double resolution);

    /**
     * Finds a run by its ID.
     */
    SubsystemRunMaster selectMasterById(@Param("discoveryRunId") Long discoveryRunId);

    /**
     * Returns the cached LLM architectural summary for a given run + model + type.
     */
    String selectLlmSummaryByConfig(
            @Param("discoveryRunId") Long   discoveryRunId,
            @Param("llmModel")       String llmModel,
            @Param("summaryType")    String summaryType);

    /**
     * Inserts a new LLM architectural summary.
     */
    void insertLlmSummary(
            @Param("discoveryRunId") Long   discoveryRunId,
            @Param("llmModel")       String llmModel,
            @Param("summaryType")    String summaryType,
            @Param("summaryText")    String summaryText);
}
