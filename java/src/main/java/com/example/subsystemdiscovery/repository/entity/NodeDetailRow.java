package com.example.subsystemdiscovery.repository.entity;

/**
 * Raw row from {@code tb_node_detail_history} for a single analysis snapshot.
 *
 * <p>Each node can have multiple detail rows, one per split level, providing
 * the decomposed name components (METHOD, CLASS, PACKAGE) for the node.
 *
 * <ul>
 *   <li>{@code nodeId}         — foreign key to {@code tb_node_history.node_id}</li>
 *   <li>{@code splitNodeLevel} — hierarchy level (0 = method, 1 = class, 2 = package)</li>
 *   <li>{@code splitNodeName}  — name fragment at this level</li>
 *   <li>{@code splitNodeType}  — type at this level (METHOD / CLASS / PACKAGE)</li>
 * </ul>
 */
public record NodeDetailRow(String nodeId, Integer splitNodeLevel,
                             String splitNodeName, String splitNodeType) {
}
