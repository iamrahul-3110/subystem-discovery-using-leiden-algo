package com.example.subsystemdiscovery.llm;

import com.example.subsystemdiscovery.llm.dto.LlmSummaryInput;
import com.example.subsystemdiscovery.discovery.dto.SubsystemDto;
import com.example.subsystemdiscovery.discovery.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.discovery.dto.SummaryType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LlmSummaryPromptBuilder {

    public String build(LlmSummaryInput input, SummaryType summaryType) {
        List<SubsystemDto> subsystems = input.subsystems() == null ? List.of() : input.subsystems();
        List<SubsystemLinkDto> links = input.subsystemLinks() == null ? List.of() : input.subsystemLinks();

        StringBuilder sb = new StringBuilder();

        // ── Role ──────────────────────────────────────────────────────────────
        sb.append("You are a senior software architect performing domain decomposition analysis.\n\n");

        // ── Run statistics ────────────────────────────────────────────────────
        sb.append("=== RUN STATISTICS ===\n")
          .append("Subsystems found: ").append(input.totalSubsystems()).append("\n")
          .append("Avg stability   : ").append(input.avgStability()).append("\n\n");

        // ── Subsystem details ─────────────────────────────────────────────────
        sb.append("=== DISCOVERED SUBSYSTEMS ===\n\n");
        for (SubsystemDto sub : subsystems) {
            sb.append("[").append(sub.id()).append("] ").append(sub.name()).append("\n")
              .append("  Nodes: ").append(sub.nodeCount())
              .append(" | Edges: ").append(sub.edgeCount())
              .append(" | Stability: ").append(sub.stabilityScore())
              .append(" | Connectivity: ").append(sub.internalConnectivity()).append("\n");

            if (StringUtils.hasText(sub.description())) {
                sb.append("  Description: ").append(sub.description()).append("\n");
            }

            // Top packages is included for LESS, MEDIUM, and COMPLETE
            if (sub.topPackages() != null && !sub.topPackages().isEmpty()) {
                sb.append("  Top packages: ").append(String.join(", ", sub.topPackages())).append("\n");
            }

            // Key nodes (CentralNodeDto), Relations, and API Endpoints are included ONLY for COMPLETE_DETAILED
            if (summaryType == SummaryType.COMPLETE_DETAILED) {
                if (sub.centralNodes() != null && !sub.centralNodes().isEmpty()) {
                    sb.append("  Key nodes: ");
                    sub.centralNodes().stream().limit(10)
                       .forEach(n -> sb.append(n.name()).append("[").append(n.type()).append("] "));
                    sb.append("\n");
                }
                if (sub.relationSummary() != null && !sub.relationSummary().isEmpty()) {
                    sb.append("  Relations: ");
                    sub.relationSummary().forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
                    sb.append("\n");
                }
                if (sub.apiEndpoints() != null && !sub.apiEndpoints().isEmpty()) {
                    sb.append("  API endpoints:\n");
                    sub.apiEndpoints().stream().limit(5).forEach(ep ->
                            sb.append("    - ")
                              .append(ep.method() != null ? ep.method() + " " : "")
                              .append(ep.path()).append("\n"));
                }
            }
            sb.append("\n");
        }

        // ── Inter-subsystem coupling ──────────────────────────────────────────
        if (!links.isEmpty()) {
            sb.append("=== INTER-SUBSYSTEM COUPLING ===\n");
            links.stream()
                 // LESS_DETAILED: show only HIGH coupling; others: show all
                 .filter(l -> summaryType != SummaryType.LESS_DETAILED || "HIGH".equals(l.couplingStrength()))
                 .forEach(link ->
                     sb.append("  ").append(link.source())
                       .append(" \u2192 ").append(link.target())
                       .append("  [").append(link.couplingStrength())
                       .append(", ").append(link.edgeCount()).append(" edges]\n"));
            sb.append("\n");
        }

        // ── Task section — different per summary type ─────────────────────────
        sb.append("=== TASK ===\n");
        switch (summaryType) {
            case LESS_DETAILED -> sb
                .append("Write a brief executive overview of the application architecture (max 150 words).\n")
                .append("Cover only: overall architecture style and the 2–3 most important subsystems.\n")
                .append("Audience: non-technical stakeholders. Keep it high-level and jargon-free.\n");

            case MEDIUM_DETAILED -> sb
                .append("Write a standard architectural summary (max 350 words) covering:\n")
                .append("  1. Overall architecture style and layer structure\n")
                .append("  2. Key responsibilities of the major subsystems\n")
                .append("  3. The most significant coupling between subsystems\n")
                .append("  4. One key improvement area\n")
                .append("Audience: engineering team leads and architects.\n");

            case COMPLETE_DETAILED -> sb
                .append("Write a comprehensive architectural deep-dive (max 600 words) covering:\n")
                .append("  1. Overall architecture style and layer structure identified\n")
                .append("  2. Detailed responsibilities of ALL subsystems\n")
                .append("  3. Full coupling analysis — which subsystems are tightly coupled and why\n")
                .append("  4. Architectural concerns and high-coupling hotspots\n")
                .append("  5. Concrete, prioritised improvement recommendations\n")
                .append("Audience: senior engineers performing architecture review.\n");
        }

        sb.append("\nRules:\n")
          .append("  - Write in plain text. No JSON, no markdown headers, no bullet lists.\n")
          .append("  - Use business-domain vocabulary, not implementation details.\n")
          .append("  - Base your analysis ONLY on the data provided above. Do not invent.\n");

        return sb.toString();
    }
}
