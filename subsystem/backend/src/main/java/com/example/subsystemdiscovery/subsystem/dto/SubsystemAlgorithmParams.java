package com.example.subsystemdiscovery.subsystem.dto;
import com.example.subsystemdiscovery.extraction.dto.RawGraphDto;

import com.example.subsystemdiscovery.leiden.model.GraphType;

import java.util.List;

/**
 * Request body for subsystem-discovery endpoints.
 *
 * <p>The three snapshot-identification params ({@code applicationId},
 * {@code applicationKey}, {@code analysisTime}) are passed as
 * {@code @RequestParam} query parameters in the URL, NOT in this body.
 *
 * <h3>Example body</h3>
 * <pre>
 * {
 *   "graphTypes":         ["CALL_GRAPH"],
 *   "runs":               20,
 *   "consensusThreshold": 0.70,
 *   "resolution":         1.0,
 *   "llmModel":           "gpt-4o",
 *   "summaryType":        "MEDIUM_DETAILED",
 *   "graphs":             null
 * }
 * </pre>
 */
public record SubsystemAlgorithmParams(

        /**
         * Graph types to load from the history tables.
         * e.g. ["CALL_GRAPH", "CODE_ASSOCIATION"]
         * When null or empty, all available graph types are loaded.
         */
        List<GraphType> graphTypes,

        /**
         * Number of independent Leiden runs used for consensus clustering.
         * More runs = more stable stability scores, slower execution.
         * Range: 1–100. Default: 20.
         */
        Integer runs,

        /**
         * Minimum fraction of runs where two nodes must co-cluster to form
         * a consensus edge. Higher = stricter, smaller subsystems.
         * Range: 0.10–0.95. Default: 0.70.
         */
        Double consensusThreshold,

        /**
         * Leiden resolution parameter — controls cluster granularity.
         * Higher = more, smaller subsystems. Lower = fewer, larger subsystems.
         * Range: 0.10–5.00. Default: 1.0.
         */
        Double resolution,

        /**
         * LLM model ID to use for architectural summary generation (API 3 only).
         * e.g. "gpt-4o", "gemini-1.5-pro".
         * When null, the server-configured default model is used.
         */
        String llmModel,

        /**
         * Controls the verbosity of the LLM-generated architectural summary.
         * LESS_DETAILED (~150 words), MEDIUM_DETAILED (~350 words, default),
         * COMPLETE_DETAILED (~600 words).
         * When null, defaults to {@link SummaryType#MEDIUM_DETAILED}.
         */
        SummaryType summaryType,

        /**
         * Optional inline graph payload for API 4 (transform endpoint only).
         * When non-null and non-empty, the DB is bypassed entirely.
         * Must be {@code null} for APIs 1–3.
         */
        List<RawGraphDto> graphs
) {
    public int runsOrDefault() {
        return runs == null || runs < 1 ? 20 : runs;
    }

    public double consensusThresholdOrDefault() {
        return consensusThreshold == null ? 0.7 : consensusThreshold;
    }

    public double resolutionOrDefault() {
        return resolution == null ? 1.0 : resolution;
    }

    public SummaryType summaryTypeOrDefault() {
        return summaryType == null ? SummaryType.MEDIUM_DETAILED : summaryType;
    }
}
