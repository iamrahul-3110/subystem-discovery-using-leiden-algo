package com.example.subsystemdiscovery.leiden.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * A weighted, undirected edge between two {@link GraphNode}s.
 *
 * <p>The edge weight accumulates contributions from each occurrence of a
 * relationship between the two nodes. Relation types (e.g. METHOD_CALL,
 * CLASS_DEPENDENCY) are derived purely from the source/target node types —
 * not from any graph-source metadata.
 */
public class WeightedEdge {
    private Long source;
    private Long target;
    private double weight;
    private int occurrenceCount;
    private Map<RelationType, Integer> relationTypes = new EnumMap<>(RelationType.class);

    public WeightedEdge() {
    }

    public WeightedEdge(Long source, Long target) {
        this.source = source;
        this.target = target;
    }

    public Long getSource() {
        return source;
    }

    public void setSource(Long source) {
        this.source = source;
    }

    public Long getTarget() {
        return target;
    }

    public void setTarget(Long target) {
        this.target = target;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public void setOccurrenceCount(int occurrenceCount) {
        this.occurrenceCount = occurrenceCount;
    }

    public Map<RelationType, Integer> getRelationTypes() {
        return relationTypes;
    }

    public void setRelationTypes(Map<RelationType, Integer> relationTypes) {
        this.relationTypes = relationTypes;
    }

    /**
     * Accumulates one occurrence of a relationship between the two nodes.
     *
     * @param weight       the contribution of this occurrence to the edge weight
     * @param relationType the type of relationship (derived from node types, never null)
     */
    public void addOccurrence(double weight, RelationType relationType) {
        this.weight += weight;
        this.occurrenceCount++;
        this.relationTypes.merge(relationType == null ? RelationType.UNKNOWN : relationType, 1, Integer::sum);
    }
}
