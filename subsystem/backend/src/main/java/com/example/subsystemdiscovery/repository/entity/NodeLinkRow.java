package com.example.subsystemdiscovery.repository.entity;

/**
 * Raw row from {@code tb_node_relation_history} for a single analysis snapshot.
 *
 * <p>Represents a directed call-graph edge between two nodes:
 * <ul>
 *   <li>{@code fromKey} — source node ID ({@code parent_node_id}, cast to VARCHAR)</li>
 *   <li>{@code toKey}   — target node ID ({@code node_id}, cast to VARCHAR)</li>
 * </ul>
 */
public record NodeLinkRow(String fromKey, String toKey) {
}
