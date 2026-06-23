package com.example.subsystemdiscovery.discovery.dto;

import java.util.List;
import java.util.Map;

/**
 * Subsystem descriptor returned by the discovery API.
 *
 * <p>Contains algorithm-produced data only. LLM fields (primaryConcern,
 * clusterType, llmConfidence) and the full node list (nodes[]) have been
 * removed — node detail is already available in the existing node tables
 * and is not needed for LLM summarisation or Mermaid graph generation.
 *
 * <h3>Fields used for LLM summary</h3>
 * id, name, description, stabilityScore, nodeCount, edgeCount,
 * internalConnectivity, topPackages, centralNodes, apiEndpoints, relationSummary
 *
 * <h3>Fields used for Mermaid graph</h3>
 * id, name, nodeCount, stabilityScore — subgraph box label
 * centralNodes — top nodes shown inside each subgraph
 * apiEndpoints — endpoint nodes inside subgraph
 */
public record SubsystemDto(

        /** Leiden-assigned cluster identifier. e.g. "cluster_1" */
        String id,

        /** Heuristic-generated 2–5 word business-domain name. e.g. "Payment Logic" */
        String name,

        /** ~40 word description of what this subsystem does. */
        String description,

        /** 0–1: fraction of Leiden runs that produced this exact cluster. 1.0 = perfectly stable. */
        double stabilityScore,

        /** Total nodes (CLASS + METHOD + PACKAGE) in this cluster. */
        int nodeCount,

        /** Internal edges between nodes within this cluster. */
        int edgeCount,

        /**
         * Actual internal edges / maximum possible edges.
         * 0 = fully sparse, 1 = fully connected.
         */
        double internalConnectivity,

        /** Top 10 Java package names by node count within this cluster. */
        List<String> topPackages,

        /**
         * Top 20 nodes ranked by internal weighted degree (CLASS + METHOD mixed).
         * Primary vocabulary for LLM and key nodes shown in Mermaid subgraph.
         */
        List<CentralNodeDto> centralNodes,

        /**
         * REST API endpoints found in this cluster (URI-typed nodes).
         * Used for Mermaid API_LAYER subgraphs and LLM context.
         */
        List<ApiEndpointDto> apiEndpoints,

        /**
         * Internal edge count by relation type.
         * e.g. {"METHOD_CALL": 142, "CLASS_DEPENDENCY": 67, "PACKAGE_CONTAINMENT": 23}
         * Tells LLM the dominant coupling style of this cluster.
         */
        Map<String, Integer> relationSummary
) {
}
