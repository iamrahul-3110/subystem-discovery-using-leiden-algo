package com.example.subsystemdiscovery.leiden.model;

/**
 * Typed enumeration of edge relationship categories in the dependency graph.
 *
 * <p>Relation types are derived purely from the source/target node types —
 * not from any graph-source metadata — and drive edge-weight assignment in
 * {@code WeightedGraphBuilder}.
 *
 * <h3>Weight table</h3>
 * <pre>
 *  METHOD_CALL             5.0   (method → method call)
 *  CLASS_METHOD_OWNERSHIP  4.5   (method ↔ class membership)
 *  CLASS_DEPENDENCY        4.0   (class → class dependency)
 *  PACKAGE_CONTAINMENT     1.5   (direct) / 1.0 (indirect)
 *  UNKNOWN                 1.5   (fallback)
 * </pre>
 */
public enum RelationType {
    METHOD_CALL,
    CLASS_METHOD_OWNERSHIP,
    CLASS_DEPENDENCY,
    PACKAGE_CONTAINMENT,
    UNKNOWN
}
