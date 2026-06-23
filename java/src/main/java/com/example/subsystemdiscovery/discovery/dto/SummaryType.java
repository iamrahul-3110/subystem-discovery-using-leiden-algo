package com.example.subsystemdiscovery.discovery.dto;

/**
 * Controls the verbosity of the LLM-generated architectural summary.
 *
 * <p>Sent by the frontend as {@code summaryType} in the API request body.
 * The backend selects a different prompt template for each level.
 *
 * <ul>
 *   <li>{@link #LESS_DETAILED}    — brief executive overview (~150 words)</li>
 *   <li>{@link #MEDIUM_DETAILED}  — standard architectural summary (~350 words) — <strong>default</strong></li>
 *   <li>{@link #COMPLETE_DETAILED}— deep-dive with coupling analysis (~600 words)</li>
 * </ul>
 */
public enum SummaryType {

    /** Brief executive overview. Covers only top-level architecture and key subsystems (~150 words). */
    LESS_DETAILED,

    /**
     * Balanced architectural summary covering subsystem responsibilities,
     * major coupling patterns, and one improvement area (~350 words).
     * <strong>Default when not specified by the user.</strong>
     */
    MEDIUM_DETAILED,

    /**
     * Comprehensive deep-dive: all subsystem responsibilities, full coupling
     * analysis, architectural concerns, and actionable improvement recommendations (~600 words).
     */
    COMPLETE_DETAILED
}
