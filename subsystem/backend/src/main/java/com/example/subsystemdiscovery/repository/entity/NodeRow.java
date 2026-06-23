package com.example.subsystemdiscovery.repository.entity;

/**
 * Raw row from {@code tb_node_history} for a single analysis snapshot.
 *
 * <p>Fields correspond directly to database columns:
 * <ul>
 *   <li>{@code nodeId}   — unique node identifier (VARCHAR cast)</li>
 *   <li>{@code nodeName} — fully-qualified or display name of the node</li>
 *   <li>{@code nodeType} — raw type string (METHOD, CLASS, PACKAGE, …)</li>
 * </ul>
 */
public record NodeRow(String nodeId, String nodeName, String nodeType) {
}
