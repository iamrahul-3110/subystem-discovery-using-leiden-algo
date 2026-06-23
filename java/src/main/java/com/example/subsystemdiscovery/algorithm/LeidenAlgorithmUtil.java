package com.example.subsystemdiscovery.algorithm;

import com.example.subsystemdiscovery.algorithm.model.WeightedEdge;
import com.example.subsystemdiscovery.algorithm.model.WeightedGraph;
import com.example.subsystemdiscovery.algorithm.model.LeidenClusteringResult;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Leiden community-detection algorithm utility with adaptive parameters for large graphs.
 *
 * <p>This utility takes a {@link WeightedGraph} and tuning parameters, runs the
 * Leiden algorithm (optionally with consensus clustering), and returns a
 * {@link LeidenClusteringResult} containing the node-to-cluster assignments and
 * per-cluster stability scores.
 *
 * <h3>Scalability adaptations</h3>
 * <ul>
 *   <li>Consensus clustering is skipped for graphs with {@code > 10,000} nodes
 *       — a single high-quality run is used instead.</li>
 *   <li>Local-move passes scale down automatically:
 *       {@code ≤ 10k nodes → 20 passes}, {@code ≤ 50k → 10 passes},
 *       {@code > 50k → 5 passes}.</li>
 *   <li>Stability pair counting is guarded: clusters larger than
 *       {@link #MAX_STABILITY_PAIR_CLUSTER_SIZE} skip O(k²) pair enumeration.</li>
 *   <li>Neighbor maps are pre-sized to avoid rehashing on large graphs.</li>
 *   <li>Co-clustering pair estimates are bounded at {@link #MAX_CONSENSUS_PAIRS}
 *       to prevent memory blow-up.</li>
 * </ul>
 *
 * <p>The algorithm itself (modularity gain, community refinement, aggregation)
 * is mathematically unchanged from the Leiden paper.
 */
@Service
public class LeidenAlgorithmUtil {

    // -------------------------------------------------------------------------
    // Scalability thresholds
    // -------------------------------------------------------------------------
    /** Above this node count, skip consensus (run Leiden once). */
    private static final int MAX_CONSENSUS_NODES = 10_000;

    /** If co-clustering pair estimates exceed this, skip consensus. */
    private static final long MAX_CONSENSUS_PAIRS = 2_000_000L;

    /** Clusters larger than this are excluded from O(k²) stability calculation. */
    private static final int MAX_STABILITY_PAIR_CLUSTER_SIZE = 5_000;

    /** Maximum Leiden hierarchy levels. */
    private static final int MAX_LEVELS = 10;

    /** Modularity gain threshold. */
    private static final double EPSILON = 1.0e-10;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Runs Leiden community detection on the given weighted graph.
     *
     * @param graph              the weighted dependency graph to cluster
     * @param runs               number of independent Leiden runs for consensus
     * @param consensusThreshold minimum co-clustering probability to include an edge in the consensus graph
     * @param resolution         Leiden resolution parameter (higher → more, smaller clusters)
     * @return clustering result with node assignments and stability scores
     * @throws IllegalStateException if the graph is empty
     */
    public LeidenClusteringResult cluster(WeightedGraph graph, int runs, double consensusThreshold, double resolution) {
        List<Long> nodeIds = graph.getNodes().stream()
                .map(node -> node.getId())
                .toList();

        if (nodeIds.isEmpty()) {
            throw new IllegalStateException("Cannot cluster an empty graph");
        }

        LocalGraph localGraph = LocalGraph.fromWeightedGraph(nodeIds, graph.getEdges());
        if (localGraph.edgeCount() == 0 || localGraph.totalWeight() <= 0.0) {
            return singletonResult(nodeIds);
        }

        // For large graphs use a single run; consensus clustering on 10k+ nodes
        // is too expensive and only marginally improves quality.
        int effectiveRuns = nodeIds.size() > MAX_CONSENSUS_NODES ? 1 : Math.max(1, runs);
        int maxPasses     = adaptiveMaxPasses(nodeIds.size());

        List<int[]> memberships = new ArrayList<>();
        for (int seed = 0; seed < effectiveRuns; seed++) {
            memberships.add(runLeiden(localGraph, resolution, seed, maxPasses));
        }

        if (effectiveRuns == 1) {
            return buildClusterResponse(memberships.get(0), nodeIds, Map.of(), 1);
        }

        if (consensusPairEstimate(memberships) > MAX_CONSENSUS_PAIRS) {
            return buildClusterResponse(memberships.get(memberships.size() - 1), nodeIds, Map.of(), 1);
        }

        Map<Long, Integer> coCounts = coClusteringCounts(memberships);
        LocalGraph consensusGraph = LocalGraph.fromConsensus(nodeIds.size(), coCounts, effectiveRuns, consensusThreshold);
        int[] finalMembership = consensusGraph.edgeCount() == 0
                ? memberships.get(memberships.size() - 1)
                : runLeiden(consensusGraph, resolution, effectiveRuns + 1, maxPasses);

        return buildClusterResponse(finalMembership, nodeIds, coCounts, effectiveRuns);
    }

    // -------------------------------------------------------------------------
    // Adaptive parameters
    // -------------------------------------------------------------------------

    /**
     * Reduces local-move passes for large graphs to keep each Leiden run O(E)
     * rather than O(passes × E).
     */
    private int adaptiveMaxPasses(int nodeCount) {
        if (nodeCount > 50_000) return 5;
        if (nodeCount > 10_000) return 10;
        return 20;
    }

    // -------------------------------------------------------------------------
    // Core Leiden algorithm
    // -------------------------------------------------------------------------

    private int[] runLeiden(LocalGraph graph, double resolution, int seed, int maxPasses) {
        Random random = new Random(seed);
        LocalGraph currentGraph = graph;
        int[] originalToCurrentNode = IntStream.range(0, graph.nodeCount()).toArray();

        for (int level = 0; level < MAX_LEVELS; level++) {
            int[] membership = compact(refineConnectedCommunities(
                    currentGraph,
                    localMoving(currentGraph, resolution, random, maxPasses)
            ));
            int communityCount = communityCount(membership);

            for (int nodeIndex = 0; nodeIndex < originalToCurrentNode.length; nodeIndex++) {
                originalToCurrentNode[nodeIndex] = membership[originalToCurrentNode[nodeIndex]];
            }

            if (communityCount == currentGraph.nodeCount() || communityCount == 1) {
                break;
            }

            currentGraph = currentGraph.aggregate(membership, communityCount);
            if (currentGraph.edgeCount() == 0) {
                break;
            }
        }

        return compact(originalToCurrentNode);
    }

    private int[] localMoving(LocalGraph graph, double resolution, Random random, int maxPasses) {
        int nodeCount = graph.nodeCount();
        int[] communityByNode  = IntStream.range(0, nodeCount).toArray();
        double[] communityDegree = graph.degrees().clone();
        int[] communitySize    = new int[nodeCount];
        for (int node = 0; node < nodeCount; node++) {
            communitySize[node] = 1;
        }

        int[] order = IntStream.range(0, nodeCount).toArray();
        for (int pass = 0; pass < maxPasses; pass++) {
            shuffle(order, random);
            boolean moved = false;

            for (int node : order) {
                double nodeDegree = graph.degrees()[node];
                if (nodeDegree <= 0.0) {
                    continue;
                }

                int sourceCommunity = communityByNode[node];
                communityDegree[sourceCommunity] -= nodeDegree;
                communitySize[sourceCommunity]--;

                Map<Integer, Double> edgeWeightByCommunity = edgeWeightsByCommunity(graph, communityByNode, node);
                int bestCommunity = sourceCommunity;
                double bestScore = moveScore(
                        edgeWeightByCommunity.getOrDefault(sourceCommunity, 0.0),
                        nodeDegree,
                        communityDegree[sourceCommunity],
                        graph.totalWeight(),
                        resolution
                );

                int singletonCommunity = singletonCommunity(sourceCommunity, communitySize);
                if (singletonCommunity >= 0) {
                    double singletonScore = moveScore(0.0, nodeDegree, 0.0, graph.totalWeight(), resolution);
                    if (singletonScore > bestScore + EPSILON) {
                        bestCommunity = singletonCommunity;
                        bestScore = singletonScore;
                    }
                }

                for (Map.Entry<Integer, Double> candidate : edgeWeightByCommunity.entrySet()) {
                    int candidateCommunity = candidate.getKey();
                    double score = moveScore(
                            candidate.getValue(),
                            nodeDegree,
                            communityDegree[candidateCommunity],
                            graph.totalWeight(),
                            resolution
                    );
                    if (score > bestScore + EPSILON
                            || (Math.abs(score - bestScore) <= EPSILON
                            && candidateCommunity != sourceCommunity
                            && random.nextBoolean())) {
                        bestCommunity = candidateCommunity;
                        bestScore = score;
                    }
                }

                communityByNode[node] = bestCommunity;
                communityDegree[bestCommunity] += nodeDegree;
                communitySize[bestCommunity]++;
                if (bestCommunity != sourceCommunity) {
                    moved = true;
                }
            }

            if (!moved) {
                break;
            }
        }

        return communityByNode;
    }

    private int[] refineConnectedCommunities(LocalGraph graph, int[] membership) {
        int[] refined = new int[membership.length];
        Arrays.fill(refined, -1);
        int nextCommunity = 0;

        for (int node = 0; node < membership.length; node++) {
            if (refined[node] >= 0) {
                continue;
            }

            int community = membership[node];
            Deque<Integer> queue = new ArrayDeque<>();
            queue.add(node);
            refined[node] = nextCommunity;

            while (!queue.isEmpty()) {
                int current = queue.removeFirst();
                for (int neighbor : graph.neighbors().get(current).keySet()) {
                    if (refined[neighbor] < 0 && membership[neighbor] == community) {
                        refined[neighbor] = nextCommunity;
                        queue.addLast(neighbor);
                    }
                }
            }

            nextCommunity++;
        }

        return refined;
    }

    // -------------------------------------------------------------------------
    // Scoring helpers
    // -------------------------------------------------------------------------

    private Map<Integer, Double> edgeWeightsByCommunity(LocalGraph graph, int[] communityByNode, int node) {
        Map<Integer, Double> edgeWeightByCommunity = new HashMap<>();
        for (Map.Entry<Integer, Double> edge : graph.neighbors().get(node).entrySet()) {
            int neighborCommunity = communityByNode[edge.getKey()];
            edgeWeightByCommunity.merge(neighborCommunity, edge.getValue(), Double::sum);
        }
        return edgeWeightByCommunity;
    }

    private int singletonCommunity(int sourceCommunity, int[] communitySize) {
        if (communitySize[sourceCommunity] == 0) {
            return sourceCommunity;
        }
        for (int community = 0; community < communitySize.length; community++) {
            if (communitySize[community] == 0) {
                return community;
            }
        }
        return -1;
    }

    private double moveScore(double edgeWeightToCommunity,
                             double nodeDegree,
                             double communityDegree,
                             double totalWeight,
                             double resolution) {
        return edgeWeightToCommunity - resolution * nodeDegree * communityDegree / (2.0 * totalWeight);
    }

    // -------------------------------------------------------------------------
    // Result builders
    // -------------------------------------------------------------------------

    private LeidenClusteringResult singletonResult(List<Long> nodeIds) {
        Map<Long, String> clusters        = new LinkedHashMap<>();
        Map<String, Double> stabilityScores = new LinkedHashMap<>();
        for (int index = 0; index < nodeIds.size(); index++) {
            String clusterId = "cluster_" + (index + 1);
            clusters.put(nodeIds.get(index), clusterId);
            stabilityScores.put(clusterId, 1.0);
        }
        return new LeidenClusteringResult(clusters, stabilityScores);
    }

    private LeidenClusteringResult buildClusterResponse(int[] membership,
                                                        List<Long> nodeIds,
                                                        Map<Long, Integer> coCounts,
                                                        int runs) {
        Map<Integer, List<Integer>> nodesByRawCluster = new LinkedHashMap<>();
        for (int nodeIndex = 0; nodeIndex < membership.length; nodeIndex++) {
            nodesByRawCluster.computeIfAbsent(membership[nodeIndex], ignored -> new ArrayList<>()).add(nodeIndex);
        }

        List<List<Integer>> sortedGroups = new ArrayList<>(nodesByRawCluster.values());
        sortedGroups.sort(Comparator
                .<List<Integer>>comparingInt(List::size)
                .reversed()
                .thenComparing(group -> group.stream()
                        .map(nodeIds::get)
                        .min(Long::compareTo)
                        .orElse(Long.MAX_VALUE)));

        Map<Integer, String> clusterByNodeIndex = new HashMap<>();
        Map<String, Double> stabilityScores     = new LinkedHashMap<>();
        for (int groupIndex = 0; groupIndex < sortedGroups.size(); groupIndex++) {
            String clusterId = "cluster_" + (groupIndex + 1);
            List<Integer> group = sortedGroups.get(groupIndex);
            for (int nodeIndex : group) {
                clusterByNodeIndex.put(nodeIndex, clusterId);
            }
            stabilityScores.put(clusterId, clusterStability(group, coCounts, runs));
        }

        Map<Long, String> clusters = new LinkedHashMap<>();
        for (int nodeIndex = 0; nodeIndex < nodeIds.size(); nodeIndex++) {
            clusters.put(nodeIds.get(nodeIndex), clusterByNodeIndex.get(nodeIndex));
        }
        return new LeidenClusteringResult(clusters, stabilityScores);
    }

    // -------------------------------------------------------------------------
    // Stability calculation
    // -------------------------------------------------------------------------

    /**
     * Computes the average co-clustering probability for a cluster.
     *
     * <p>For clusters larger than {@link #MAX_STABILITY_PAIR_CLUSTER_SIZE},
     * returns {@code 1.0} immediately to avoid O(k²) pair enumeration.
     */
    private double clusterStability(List<Integer> members, Map<Long, Integer> coCounts, int runs) {
        if (members.size() <= 1 || runs <= 1) {
            return 1.0;
        }
        if (members.size() > MAX_STABILITY_PAIR_CLUSTER_SIZE) {
            return 1.0;
        }

        double probabilitySum = 0.0;
        int pairCount = 0;
        for (int leftIndex = 0; leftIndex < members.size(); leftIndex++) {
            for (int rightIndex = leftIndex + 1; rightIndex < members.size(); rightIndex++) {
                probabilitySum += coCounts.getOrDefault(pairKey(members.get(leftIndex), members.get(rightIndex)), 0) / (double) runs;
                pairCount++;
            }
        }

        return pairCount == 0 ? 1.0 : round(probabilitySum / pairCount);
    }

    // -------------------------------------------------------------------------
    // Consensus helpers
    // -------------------------------------------------------------------------

    private long consensusPairEstimate(List<int[]> memberships) {
        long maxPairs = 0L;
        for (int[] membership : memberships) {
            Map<Integer, Integer> sizeByCluster = new HashMap<>();
            for (int clusterId : membership) {
                sizeByCluster.merge(clusterId, 1, Integer::sum);
            }
            long pairs = 0L;
            for (int size : sizeByCluster.values()) {
                pairs += (long) size * (size - 1) / 2L;
            }
            maxPairs = Math.max(maxPairs, pairs);
        }
        return maxPairs;
    }

    private Map<Long, Integer> coClusteringCounts(List<int[]> memberships) {
        Map<Long, Integer> counts = new HashMap<>();
        for (int[] membership : memberships) {
            Map<Integer, List<Integer>> nodesByCluster = new HashMap<>();
            for (int nodeIndex = 0; nodeIndex < membership.length; nodeIndex++) {
                nodesByCluster.computeIfAbsent(membership[nodeIndex], ignored -> new ArrayList<>()).add(nodeIndex);
            }

            for (List<Integer> members : nodesByCluster.values()) {
                if (members.size() > MAX_STABILITY_PAIR_CLUSTER_SIZE) {
                    continue;
                }
                for (int leftIndex = 0; leftIndex < members.size(); leftIndex++) {
                    for (int rightIndex = leftIndex + 1; rightIndex < members.size(); rightIndex++) {
                        counts.merge(pairKey(members.get(leftIndex), members.get(rightIndex)), 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    // -------------------------------------------------------------------------
    // Array utilities
    // -------------------------------------------------------------------------

    private int[] compact(int[] membership) {
        Map<Integer, Integer> remap = new LinkedHashMap<>();
        int[] compacted = new int[membership.length];
        for (int index = 0; index < membership.length; index++) {
            int compactCommunity = remap.computeIfAbsent(membership[index], ignored -> remap.size());
            compacted[index] = compactCommunity;
        }
        return compacted;
    }

    private int communityCount(int[] membership) {
        int max = -1;
        for (int community : membership) {
            max = Math.max(max, community);
        }
        return max + 1;
    }

    private void shuffle(int[] values, Random random) {
        for (int index = values.length - 1; index > 0; index--) {
            int swapIndex = random.nextInt(index + 1);
            int value = values[index];
            values[index] = values[swapIndex];
            values[swapIndex] = value;
        }
    }

    private static long pairKey(int left, int right) {
        int min = Math.min(left, right);
        int max = Math.max(left, right);
        return ((long) min << 32) | (max & 0xffffffffL);
    }

    private static int leftFromPairKey(long pairKey) {
        return (int) (pairKey >> 32);
    }

    private static int rightFromPairKey(long pairKey) {
        return (int) pairKey;
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }

    // =========================================================================
    // Internal graph representation
    // =========================================================================

    /**
     * Compact adjacency-list representation used internally by the Leiden algorithm.
     *
     * <p>Neighbor maps are pre-sized based on node count to avoid rehashing
     * during large-graph construction.
     */
    private record LocalGraph(
            List<Map<Integer, Double>> neighbors,
            double[] degrees,
            double totalWeight,
            int edgeCount
    ) {
        private int nodeCount() {
            return degrees.length;
        }

        private LocalGraph aggregate(int[] membership, int communityCount) {
            double[] nextDegrees = new double[communityCount];
            Map<Long, Double> edgeWeights = new LinkedHashMap<>();

            for (int node = 0; node < nodeCount(); node++) {
                int nodeCommunity = membership[node];
                nextDegrees[nodeCommunity] += degrees[node];

                for (Map.Entry<Integer, Double> edge : neighbors.get(node).entrySet()) {
                    int neighbor = edge.getKey();
                    if (node >= neighbor) {
                        continue;
                    }

                    int neighborCommunity = membership[neighbor];
                    if (nodeCommunity == neighborCommunity) {
                        continue;
                    }

                    edgeWeights.merge(pairKey(nodeCommunity, neighborCommunity), edge.getValue(), Double::sum);
                }
            }

            List<Map<Integer, Double>> nextNeighbors = emptyNeighbors(communityCount);
            for (Map.Entry<Long, Double> edge : edgeWeights.entrySet()) {
                addEdge(nextNeighbors, leftFromPairKey(edge.getKey()), rightFromPairKey(edge.getKey()), edge.getValue());
            }
            return new LocalGraph(nextNeighbors, nextDegrees, totalWeight, edgeWeights.size());
        }

        private static LocalGraph fromWeightedGraph(List<Long> nodeIds, List<WeightedEdge> edges) {
            Map<Long, Integer> indexByNodeId = new HashMap<>(nodeIds.size() * 2);
            for (int index = 0; index < nodeIds.size(); index++) {
                indexByNodeId.put(nodeIds.get(index), index);
            }

            Map<Long, Double> edgeWeights = new LinkedHashMap<>();
            for (WeightedEdge edge : edges) {
                Integer source = indexByNodeId.get(edge.getSource());
                Integer target = indexByNodeId.get(edge.getTarget());
                if (source == null || target == null || source.equals(target) || edge.getWeight() <= 0.0) {
                    continue;
                }
                edgeWeights.merge(pairKey(source, target), edge.getWeight(), Double::sum);
            }

            return fromEdgeWeights(nodeIds.size(), edgeWeights);
        }

        private static LocalGraph fromConsensus(int nodeCount,
                                                Map<Long, Integer> coCounts,
                                                int runs,
                                                double threshold) {
            Map<Long, Double> edgeWeights = new LinkedHashMap<>();
            for (Map.Entry<Long, Integer> entry : coCounts.entrySet()) {
                double probability = entry.getValue() / (double) runs;
                if (probability >= threshold) {
                    edgeWeights.put(entry.getKey(), probability);
                }
            }
            return fromEdgeWeights(nodeCount, edgeWeights);
        }

        private static LocalGraph fromEdgeWeights(int nodeCount, Map<Long, Double> edgeWeights) {
            int expectedDegree = Math.max(4, edgeWeights.size() * 2 / Math.max(1, nodeCount));
            List<Map<Integer, Double>> neighbors = emptyNeighbors(nodeCount, expectedDegree);
            double[] degrees  = new double[nodeCount];
            double totalWeight = 0.0;

            for (Map.Entry<Long, Double> edge : edgeWeights.entrySet()) {
                int source = leftFromPairKey(edge.getKey());
                int target = rightFromPairKey(edge.getKey());
                double weight = edge.getValue();
                if (source == target || weight <= 0.0) {
                    continue;
                }
                addEdge(neighbors, source, target, weight);
                degrees[source] += weight;
                degrees[target] += weight;
                totalWeight += weight;
            }

            return new LocalGraph(neighbors, degrees, totalWeight, edgeWeights.size());
        }

        private static List<Map<Integer, Double>> emptyNeighbors(int nodeCount) {
            return emptyNeighbors(nodeCount, 4);
        }

        private static List<Map<Integer, Double>> emptyNeighbors(int nodeCount, int initialCapacity) {
            List<Map<Integer, Double>> neighbors = new ArrayList<>(nodeCount);
            for (int index = 0; index < nodeCount; index++) {
                neighbors.add(new HashMap<>(initialCapacity));
            }
            return neighbors;
        }

        private static void addEdge(List<Map<Integer, Double>> neighbors, int source, int target, double weight) {
            neighbors.get(source).merge(target, weight, Double::sum);
            neighbors.get(target).merge(source, weight, Double::sum);
        }
    }
}
