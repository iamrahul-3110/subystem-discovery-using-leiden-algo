package com.example.subsystemdiscovery.discovery.dto;

/**
 * Represents a coupling relationship between two discovered subsystems.
 *
 * <p>Used for:
 * <ul>
 *   <li>LLM prompt context — coupling type and strength between subsystems</li>
 *   <li>Mermaid graph — arrow between subgraph boxes, labelled with edgeCount and couplingStrength</li>
 * </ul>
 *
 * <p>Note: {@code weight} (raw sum of edge weights) has been removed — it is a
 * technical internal value not meaningful to the LLM or Mermaid renderer.
 * {@code couplingStrength} (HIGH / MEDIUM / LOW) is the human-readable equivalent.
 */
public record SubsystemLinkDto(

        /** Source cluster ID. e.g. "cluster_1" */
        String source,

        /** Target cluster ID. e.g. "cluster_3" */
        String target,

        /**
         * Number of cross-cluster edges between source and target.
         * Used as the arrow label in Mermaid diagrams.
         */
        int edgeCount,

        /**
         * Human-readable coupling level derived from weight + edgeCount thresholds:
         * HIGH (weight≥20 or edges≥10), MEDIUM (weight≥8 or edges≥4), LOW otherwise.
         * Used to style Mermaid arrows: ==> for HIGH, --> for MEDIUM, -.-> for LOW.
         */
        String couplingStrength
) {
}

