package com.example.subsystemdiscovery.leiden.model;

/**
 * Enumerates the types of graphs that can be loaded from the history tables.
 *
 * <p>In v2, {@code graphType} is no longer used to drive clustering decisions.
 * All relation-type and edge-weight logic is determined by node types
 * (CLASS / METHOD / PACKAGE). This enum is retained for DB-partition queries
 * and backward compatibility with API request bodies.
 */
public enum GraphType {
    CALL_GRAPH,
    CODE_ASSOCIATION
}
