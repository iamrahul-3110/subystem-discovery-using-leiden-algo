package com.example.subsystemdiscovery.leiden;

import com.example.subsystemdiscovery.subsystem.dto.ApiEndpointDto;
import com.example.subsystemdiscovery.subsystem.dto.CentralNodeDto;
import com.example.subsystemdiscovery.subsystem.dto.NodeAssignmentDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.leiden.model.GraphNode;
import com.example.subsystemdiscovery.leiden.model.RelationType;
import com.example.subsystemdiscovery.leiden.model.WeightedEdge;
import com.example.subsystemdiscovery.leiden.model.WeightedGraph;
import com.example.subsystemdiscovery.leiden.model.ClusterAggregation;
import com.example.subsystemdiscovery.leiden.model.LeidenClusteringResult;
import com.example.subsystemdiscovery.leiden.model.SubsystemDraft;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Aggregates Leiden clustering results into rich {@link SubsystemDraft} objects.
 *
 * <p>Takes the raw {@link LeidenClusteringResult} (node→cluster assignments) and
 * the original {@link WeightedGraph}, then builds:
 * <ul>
 *   <li>One {@link SubsystemDraft} per cluster with centralNodes, topPackages,
 *       apiEndpoints, classNodes, methodNodes, and relationSummary.</li>
 *   <li>Inter-cluster {@link SubsystemLinkDto} list (coupling summary).</li>
 *   <li>Flat {@link NodeAssignmentDto} list for every node.</li>
 * </ul>
 *
 * <h3>Node-centric approach</h3>
 * <ul>
 *   <li>CLASS and METHOD nodes are tracked separately so the LLM prompt can
 *       describe them independently.</li>
 *   <li>A {@code relationSummary} (edge-type breakdown) is attached to each
 *       draft so the LLM knows whether this cluster is call-heavy, import-heavy,
 *       or structurally driven.</li>
 *   <li>{@code nodeSummaries} are capped at {@link #MAX_NODE_SUMMARIES} to keep
 *       DTO sizes manageable for large clusters (10k–100k node graphs).</li>
 * </ul>
 */
@Service
public class ClusterAggregationUtil {


    /** Maximum central-node candidates (all types mixed) shown in the draft. */
    private static final int MAX_CENTRAL_NODES = 20;

    /** Maximum class-type and method-type node entries per draft. */
    private static final int MAX_TYPE_NODES = 15;

    /** Maximum top packages per cluster. */
    private static final int MAX_TOP_PACKAGES = 10;

    private static final Pattern METHOD_PATH_PATTERN = Pattern.compile(
            "^(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)\\s+(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATH_METHOD_PATTERN = Pattern.compile(
            "^(.+):(GET|POST|PUT|PATCH|DELETE|HEAD|OPTIONS)$", Pattern.CASE_INSENSITIVE);

    public ClusterAggregation aggregate(WeightedGraph graph, LeidenClusteringResult clusteringResult) {
        Map<Long, GraphNode> nodeById = graph.getNodes().stream()
                .collect(Collectors.toMap(GraphNode::getId, Function.identity(),
                        (left, right) -> left, LinkedHashMap::new));
        Map<Long, String> clusterByNode = clusteringResult.getClusters();

        // Group nodes by cluster
        Map<String, List<GraphNode>> nodesByCluster = new LinkedHashMap<>();
        for (GraphNode node : graph.getNodes()) {
            String clusterId = clusterByNode.getOrDefault(node.getId(), "subsystem-unassigned");
            nodesByCluster.computeIfAbsent(clusterId, ignored -> new ArrayList<>()).add(node);
        }

        // Per-node internal weighted degree and per-cluster edge counts
        Map<Long, Double> internalWeightedDegree     = new HashMap<>();
        Map<String, Integer> internalEdgeCountByCluster = new HashMap<>();
        // Per-cluster relation-type counts (internal edges only)
        Map<String, Map<RelationType, Integer>> internalRelationsByCluster = new HashMap<>();
        // Inter-cluster links
        Map<String, InterClusterAccumulator> interClusterLinks = new LinkedHashMap<>();

        for (WeightedEdge edge : graph.getEdges()) {
            String sourceCluster = clusterByNode.get(edge.getSource());
            String targetCluster = clusterByNode.get(edge.getTarget());
            if (sourceCluster == null || targetCluster == null) {
                continue;
            }
            if (sourceCluster.equals(targetCluster)) {
                internalEdgeCountByCluster.merge(sourceCluster, 1, Integer::sum);
                internalWeightedDegree.merge(edge.getSource(), edge.getWeight(), Double::sum);
                internalWeightedDegree.merge(edge.getTarget(), edge.getWeight(), Double::sum);
                // Accumulate relation-type breakdown per cluster
                Map<RelationType, Integer> relMap = internalRelationsByCluster
                        .computeIfAbsent(sourceCluster, k -> new EnumMap<>(RelationType.class));
                edge.getRelationTypes().forEach((rel, cnt) -> relMap.merge(rel, cnt, Integer::sum));
            } else {
                String left  = sourceCluster.compareTo(targetCluster) <= 0 ? sourceCluster : targetCluster;
                String right = sourceCluster.compareTo(targetCluster) <= 0 ? targetCluster : sourceCluster;
                String key   = left + "--" + right;
                interClusterLinks.computeIfAbsent(key, ignored -> new InterClusterAccumulator(left, right))
                        .add(edge);
            }
        }

        List<SubsystemDraft> subsystemDrafts = nodesByCluster.entrySet().stream()
                .map(entry -> buildSubsystemDraft(
                        entry.getKey(),
                        entry.getValue(),
                        internalWeightedDegree,
                        internalEdgeCountByCluster.getOrDefault(entry.getKey(), 0),
                        clusteringResult.getStabilityScores().getOrDefault(entry.getKey(), 1.0),
                        internalRelationsByCluster.getOrDefault(entry.getKey(), Map.of())
                ))
                .sorted(Comparator.comparing(SubsystemDraft::getNodeCount).reversed()
                        .thenComparing(SubsystemDraft::getId))
                .toList();

        List<SubsystemLinkDto> subsystemLinks = interClusterLinks.values().stream()
                .map(InterClusterAccumulator::toDto)
                .sorted(Comparator.comparing(SubsystemLinkDto::edgeCount).reversed())
                .toList();

        List<NodeAssignmentDto> assignments = graph.getNodes().stream()
                .map(node -> new NodeAssignmentDto(node.getId(),
                        clusterByNode.getOrDefault(node.getId(), "subsystem-unassigned"), 1.0))
                .toList();

        ClusterAggregation aggregation = new ClusterAggregation();
        aggregation.setSubsystemDrafts(subsystemDrafts);
        aggregation.setSubsystemLinks(subsystemLinks);
        aggregation.setNodeAssignments(assignments);
        return aggregation;
    }

    private SubsystemDraft buildSubsystemDraft(String clusterId,
                                               List<GraphNode> nodes,
                                               Map<Long, Double> internalWeightedDegree,
                                               int internalEdgeCount,
                                               double stabilityScore,
                                               Map<RelationType, Integer> internalRelations) {
        double detectedMaxDegree = nodes.stream()
                .mapToDouble(n -> internalWeightedDegree.getOrDefault(n.getId(), 0.0))
                .max()
                .orElse(1.0);
        final double maxDegree = detectedMaxDegree <= 0.0 ? 1.0 : detectedMaxDegree;

        // ── Build scored node candidates ──────────────────────────────────────
        List<CentralNodeDto> scoredCandidates = new ArrayList<>();
        List<CentralNodeDto> scoredClasses    = new ArrayList<>();
        List<CentralNodeDto> scoredMethods    = new ArrayList<>();

        for (GraphNode node : nodes) {
            if (node.isPackage()) {
                continue; // Packages are structural — exclude from scoring lists
            }
            double rawScore = internalWeightedDegree.getOrDefault(node.getId(), 0.0) / maxDegree;
            // Apply a type-based bonus so METHOD nodes surface more prominently:
            // methods carry the strongest call-graph signal.
            double typeBonus = node.isMethod() ? 1.2 : 1.0;
            double score     = round(rawScore * typeBonus);

            CentralNodeDto dto = new CentralNodeDto(
                    node.getId(),
                    node.getName(),
                    node.getQualifiedName(),
                    node.getType(),
                    score
            );
            scoredCandidates.add(dto);
            if (node.isMethod()) {
                scoredMethods.add(dto);
            } else {
                scoredClasses.add(dto);
            }
        }

        Comparator<CentralNodeDto> byScoreDesc =
                Comparator.comparing(CentralNodeDto::score).reversed()
                          .thenComparing(CentralNodeDto::name);

        List<CentralNodeDto> centralNodes = scoredCandidates.stream()
                .sorted(byScoreDesc)
                .limit(MAX_CENTRAL_NODES)
                .toList();

        List<CentralNodeDto> classNodes = scoredClasses.stream()
                .sorted(byScoreDesc)
                .limit(MAX_TYPE_NODES)
                .toList();

        List<CentralNodeDto> methodNodes = scoredMethods.stream()
                .sorted(byScoreDesc)
                .limit(MAX_TYPE_NODES)
                .toList();

        // ── Top packages (up to 10) ───────────────────────────────────────────
        List<String> topPackages = nodes.stream()
                .map(GraphNode::getPackageName)
                .filter(StringUtils::hasText)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(MAX_TOP_PACKAGES)
                .map(Map.Entry::getKey)
                .toList();

        // ── API endpoints (URI-typed nodes) ───────────────────────────────────
        List<ApiEndpointDto> apiEndpoints = nodes.stream()
                .filter(n -> "URI".equalsIgnoreCase(n.getType()))
                .map(this::toApiEndpoint)
                .limit(20)
                .toList();

        // ── Relation-type summary ─────────────────────────────────────────────
        Map<String, Integer> relationSummary = new LinkedHashMap<>();
        internalRelations.entrySet().stream()
                .sorted(Map.Entry.<RelationType, Integer>comparingByValue().reversed())
                .forEach(e -> relationSummary.put(e.getKey().name(), e.getValue()));

        // ── Connectivity metrics ──────────────────────────────────────────────
        int nodeCount = nodes.size();
        double possibleEdges = nodeCount <= 1 ? 1.0 : (nodeCount * (nodeCount - 1)) / 2.0;

        // ── Assemble draft ────────────────────────────────────────────────────
        SubsystemDraft draft = new SubsystemDraft();
        draft.setId(clusterId);
        draft.setStabilityScore(round(stabilityScore));
        draft.setNodeCount(nodeCount);
        draft.setEdgeCount(internalEdgeCount);
        draft.setInternalConnectivity(round(internalEdgeCount / possibleEdges));
        draft.setTopPackages(topPackages);
        draft.setCentralNodes(centralNodes);
        draft.setClassNodes(classNodes);
        draft.setMethodNodes(methodNodes);
        draft.setApiEndpoints(apiEndpoints);
        draft.setRelationSummary(relationSummary);
        return draft;
    }

    private ApiEndpointDto toApiEndpoint(GraphNode node) {
        String value = node.getQualifiedName();
        Matcher methodPath = METHOD_PATH_PATTERN.matcher(value);
        if (methodPath.matches()) {
            return new ApiEndpointDto(node.getId(), methodPath.group(1).toUpperCase(Locale.ROOT), methodPath.group(2));
        }
        Matcher pathMethod = PATH_METHOD_PATTERN.matcher(value);
        if (pathMethod.matches()) {
            return new ApiEndpointDto(node.getId(), pathMethod.group(2).toUpperCase(Locale.ROOT), pathMethod.group(1));
        }
        return new ApiEndpointDto(node.getId(), null, value);
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
    private static class InterClusterAccumulator {
        private final String source;
        private final String target;
        private double weight;
        private int edgeCount;

        private InterClusterAccumulator(String source, String target) {
            this.source = source;
            this.target = target;
        }

        private void add(WeightedEdge edge) {
            this.weight += edge.getWeight();
            this.edgeCount += edge.getOccurrenceCount();
        }

        private SubsystemLinkDto toDto() {
            return new SubsystemLinkDto(source, target, edgeCount,
                    couplingStrength(weight, edgeCount));
        }

        private String couplingStrength(double weight, int edgeCount) {
            if (weight >= 20.0 || edgeCount >= 10) return "HIGH";
            if (weight >= 8.0  || edgeCount >= 4)  return "MEDIUM";
            return "LOW";
        }
    }

}
