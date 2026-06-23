package com.example.subsystemdiscovery.controller;

import com.example.subsystemdiscovery.leiden.dto.LeidenInputDto;
import com.example.subsystemdiscovery.llm.dto.LlmDiscoveryResponse;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemAlgorithmParams;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDiscoveryResponse;
import com.example.subsystemdiscovery.subsystem.dto.SummaryType;
import com.example.subsystemdiscovery.subsystem.SubsystemDiscoveryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing exactly 4 subsystem-discovery APIs.
 *
 * <p>The three snapshot-identification parameters are passed as
 * <strong>query parameters</strong> ({@code @RequestParam}) in the URL:
 * <ul>
 *   <li>{@code applicationId}  – numeric ID matching {@code tb_node_history.application_id}</li>
 *   <li>{@code applicationKey} – human-readable label (used in LLM prompt context)</li>
 *   <li>{@code analysisTime}   – snapshot key matching {@code tb_node_history.analysis_time}</li>
 * </ul>
 *
 * <p>The request body ({@link SubsystemAlgorithmParams}) carries only the
 * algorithm-tuning knobs ({@code graphTypes}, {@code runs}, etc.).
 *
 * <h3>API Summary</h3>
 * <pre>
 *  API 1  POST /api/codeanalyzer/subsystem/leiden-input
 *         Get the weighted graph (no Leiden run) — validate data before full run
 *
 *  API 2  POST /api/codeanalyzer/subsystem/discover
 *         Run Leiden clustering → SubsystemDiscoveryResponse
 *
 *  API 3  POST /api/codeanalyzer/subsystem/discover-llm
 *         Run Leiden + send all clusters to custom LLM → LlmDiscoveryResponse
 *
 *  API 4  POST /api/codeanalyzer/subsystem/transform/leiden-input
 *         Transform inline JSON graphs to LeidenInputDto (no DB, no Leiden run)
 *         (applicationId / applicationKey / analysisTime optional when graphs != null)
 * </pre>
 */
@RestController
@RequestMapping("/api/codeanalyzer/subsystem")
public class SubsystemDiscoveryController {

    private final SubsystemDiscoveryService subsystemDiscoveryService;

    public SubsystemDiscoveryController(SubsystemDiscoveryService subsystemDiscoveryService) {
        this.subsystemDiscoveryService = subsystemDiscoveryService;
    }

    // =========================================================================
    // API 1 — Get Leiden Input from history tables (preview / validate data)
    // =========================================================================

    /**
     * Loads the history-table snapshot and returns the weighted graph as
     * {@link LeidenInputDto} — WITHOUT running the Leiden algorithm.
     *
     * <p>Use this first to verify that a specific {@code analysisTime} snapshot
     * contains the expected number of nodes and edges before triggering a full run.
     *
     * @param applicationId  numeric application ID
     * @param applicationKey human-readable application label
     * @param analysisTime   analysis snapshot key (must match a value in {@code tb_node_history.analysis_time})
     * @param params         algorithm tuning (graphTypes etc.; runs/threshold ignored for this endpoint)
     */
    @PostMapping("/leiden-input")
    public ResponseEntity<LeidenInputDto> getLeidenInput(
            @RequestParam("analysisTime")   String analysisTime,
            @RequestBody  SubsystemAlgorithmParams  params) {

        return ResponseEntity.ok(
                subsystemDiscoveryService.toLeidenInput(analysisTime, params));
    }

    // =========================================================================
    // API 2 — Discover Subsystems from history tables (Leiden only)
    // =========================================================================

    /**
     * Runs the full Leiden community-detection pipeline on the history snapshot
     * identified by {@code analysisTime}.
     *
     * @param analysisTime   analysis snapshot key
     * @param params         algorithm tuning (runs, consensusThreshold, resolution, …)
     */
    @PostMapping("/discover")
    public ResponseEntity<SubsystemDiscoveryResponse> discoverSubsystems(
            @RequestParam("analysisTime")   String analysisTime,
            @RequestBody  SubsystemAlgorithmParams  params) {

        return ResponseEntity.ok(
                subsystemDiscoveryService.discover(analysisTime, params));
    }

    // =========================================================================
    // API 3 — Discover Subsystems with LLM enrichment (Leiden + custom LLM)
    // =========================================================================

    /**
     * Runs the full Leiden pipeline on the history snapshot, then sends all
     * discovered subsystems to the custom LLM chat-completions API for
     * architectural enrichment.
     *
     * <p>Requires {@code subsystem.llm.enabled=true} in {@code application.yml}.
     * Falls back to heuristic labels gracefully when LLM is unavailable.
     *
     * @param analysisTime   analysis snapshot key
     * @param params         algorithm tuning
     */
    @PostMapping("/discover-llm")
    public ResponseEntity<LlmDiscoveryResponse> discoverSubsystemsWithLlm(
            @RequestParam("analysisTime")   String analysisTime,
            @RequestBody  SubsystemAlgorithmParams  params) {

        return ResponseEntity.ok(
                subsystemDiscoveryService.discoverWithLlm(analysisTime, params));
    }

    // =========================================================================
    // API 5 — Generate/re-summarize an architectural summary
    // =========================================================================

    /**
     * Generates or retrieves an architectural summary for an existing discovery run.
     *
     * @param discoveryRunId the ID of the existing discovery run
     * @param llmModel       optional LLM model ID
     * @param summaryType    optional summary type / verbosity
     */
    @PostMapping("/summary")
    public ResponseEntity<String> generateSummary(
            @RequestParam("discoveryRunId") Long discoveryRunId,
            @RequestParam(value = "llmModel", required = false) String llmModel,
            @RequestParam(value = "summaryType", required = false) String summaryType) {

        SummaryType resolvedSummaryType = parseSummaryType(summaryType);
        return ResponseEntity.ok(
                subsystemDiscoveryService.generateSummary(discoveryRunId, llmModel, resolvedSummaryType));
    }

    private SummaryType parseSummaryType(String value) {
        if (value == null || value.trim().isEmpty()) {
            return SummaryType.MEDIUM_DETAILED;
        }
        return switch (value.toUpperCase()) {
            case "SMALL", "LESS", "LESS_DETAILED" -> SummaryType.LESS_DETAILED;
            case "LARGE", "COMPLETE", "COMPLETE_DETAILED" -> SummaryType.COMPLETE_DETAILED;
            default -> SummaryType.MEDIUM_DETAILED;
        };
    }

    // =========================================================================
    // API 4 — Transform inline JSON to Leiden Input (no DB required)
    // =========================================================================

    /**
     * Accepts a graph payload embedded in the request body ({@code params.graphs()})
     * and returns the normalised {@link LeidenInputDto} — no database access, no algorithm run.
     *
     * <p>When {@code params.graphs()} is null/empty and analysisTime query param is
     * provided, falls back to loading from the history tables (same as API 1).
     *
     * @param analysisTime   optional — only needed when {@code params.graphs()} is null
     * @param params         must contain a non-empty {@code graphs} list for pure inline mode
     */
    @PostMapping("/transform/leiden-input")
    public ResponseEntity<LeidenInputDto> transformToLeidenInput(
            @RequestParam(value = "analysisTime",   required = false) String analysisTime,
            @RequestBody SubsystemAlgorithmParams params) {

        return ResponseEntity.ok(
                subsystemDiscoveryService.toLeidenInput(analysisTime, params));
    }
}
