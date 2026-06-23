package com.example.subsystemdiscovery.algorithm.model;

import com.example.subsystemdiscovery.discovery.dto.ApiEndpointDto;
import com.example.subsystemdiscovery.discovery.dto.CentralNodeDto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable intermediate holder for all information gathered about a single
 * Leiden cluster before the label is applied by {@code SubsystemLabelService}.
 *
 * <p>The separation of {@code classNodes} and {@code methodNodes} lets the
 * LLM prompt describe the two concerns independently, producing richer labels.
 * The {@code relationSummary} tells the LLM whether this cluster is dominated
 * by method calls (business-logic layer), class dependencies (service/domain
 * layer), or package containment (infrastructure).
 *
 * <p>Produced by {@code ClusterAggregationUtil} and consumed by
 * {@code SubsystemDiscoveryService} via {@code SubsystemLabelService}.
 */
public class SubsystemDraft {
    private String id;
    private double stabilityScore;
    private int nodeCount;
    private int edgeCount;
    private double internalConnectivity;

    /** Top packages ranked by node density (up to 10). */
    private List<String> topPackages = new ArrayList<>();

    /** Top nodes by internal weighted degree — all types mixed, up to 20. */
    private List<CentralNodeDto> centralNodes = new ArrayList<>();

    /** Top CLASS nodes by internal weighted degree (up to 15). */
    private List<CentralNodeDto> classNodes = new ArrayList<>();

    /** Top METHOD nodes by internal weighted degree (up to 15). */
    private List<CentralNodeDto> methodNodes = new ArrayList<>();

    /** API endpoint nodes found in this cluster (URI-typed nodes, up to 20). */
    private List<ApiEndpointDto> apiEndpoints = new ArrayList<>();


    /**
     * Count of internal edges by relation-type name.
     * e.g. {@code {"METHOD_CALL": 142, "CLASS_DEPENDENCY": 67, ...}}
     */
    private Map<String, Integer> relationSummary = new LinkedHashMap<>();

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getStabilityScore() { return stabilityScore; }
    public void setStabilityScore(double stabilityScore) { this.stabilityScore = stabilityScore; }

    public int getNodeCount() { return nodeCount; }
    public void setNodeCount(int nodeCount) { this.nodeCount = nodeCount; }

    public int getEdgeCount() { return edgeCount; }
    public void setEdgeCount(int edgeCount) { this.edgeCount = edgeCount; }

    public double getInternalConnectivity() { return internalConnectivity; }
    public void setInternalConnectivity(double internalConnectivity) { this.internalConnectivity = internalConnectivity; }

    public List<String> getTopPackages() { return topPackages; }
    public void setTopPackages(List<String> topPackages) { this.topPackages = topPackages; }

    public List<CentralNodeDto> getCentralNodes() { return centralNodes; }
    public void setCentralNodes(List<CentralNodeDto> centralNodes) { this.centralNodes = centralNodes; }

    public List<CentralNodeDto> getClassNodes() { return classNodes; }
    public void setClassNodes(List<CentralNodeDto> classNodes) { this.classNodes = classNodes; }

    public List<CentralNodeDto> getMethodNodes() { return methodNodes; }
    public void setMethodNodes(List<CentralNodeDto> methodNodes) { this.methodNodes = methodNodes; }

    public List<ApiEndpointDto> getApiEndpoints() { return apiEndpoints; }
    public void setApiEndpoints(List<ApiEndpointDto> apiEndpoints) { this.apiEndpoints = apiEndpoints; }


    public Map<String, Integer> getRelationSummary() { return relationSummary; }
    public void setRelationSummary(Map<String, Integer> relationSummary) { this.relationSummary = relationSummary; }
}
