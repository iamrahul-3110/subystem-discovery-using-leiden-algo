package com.example.subsystemdiscovery.algorithm.model;

/**
 * The result of labelling a Leiden cluster — either from the LLM or from heuristics.
 *
 * <p>Produced by {@code SubsystemLabelService} and embedded into each
 * {@code SubsystemDto} in the final discovery response.
 *
 * @param name           2–5 word business/domain name for the subsystem
 * @param description    ≤ 40 word description of what the subsystem does
 * @param confidence     0.0–1.0 — how certain the labeller is (1.0 = very obvious)
 * @param primaryConcern one-sentence core business domain of this cluster (may be null for heuristic labels)
 * @param clusterType    dominant architectural role: BUSINESS_LOGIC | DATA_ACCESS | API_LAYER |
 *                       INFRASTRUCTURE | UTILITY | MIXED (may be null for heuristic labels)
 */
public record LabelResult(
        String name,
        String description,
        double confidence,
        String primaryConcern,
        String clusterType
) {
    /** Convenience constructor for heuristic / fallback labels that lack LLM-specific fields. */
    public LabelResult(String name, String description, double confidence) {
        this(name, description, confidence, null, null);
    }
}
