package com.example.subsystemdiscovery.llm.dto;
import com.example.subsystemdiscovery.subsystem.dto.AlgorithmInfoDto;
import com.example.subsystemdiscovery.subsystem.dto.NodeAssignmentDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.subsystem.dto.SummaryDto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * Response returned by the LLM-enriched subsystem discovery endpoint (API 3).
 *
 * <p>Contains the standard Leiden algorithm results PLUS a single LLM-generated
 * architectural summary for the entire application. The LLM receives the complete
 * {@link SubsystemDiscoveryResponse} and produces one cohesive summary — it does
 * not generate per-subsystem descriptions.
 */
public record LlmDiscoveryResponse(
        @JsonIgnore Long discoveryRunId,
        @JsonIgnore Long applicationId,
        @JsonIgnore String applicationKey,
        String analysisTime,
        @JsonIgnore AlgorithmInfoDto algorithm,
        SummaryDto summary,
        List<SubsystemDto> subsystems,
        List<SubsystemLinkDto> subsystemLinks,
        @JsonIgnore List<NodeAssignmentDto> nodeAssignments,
        String llmArchitecturalSummary
) {
}

