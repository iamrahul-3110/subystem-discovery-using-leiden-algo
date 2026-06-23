package com.example.subsystemdiscovery.leiden.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A node in the weighted dependency graph.
 *
 * <p>Node {@code type} is normalised to one of three canonical values:
 * <ul>
 *   <li>{@code "PACKAGE"} — a Java package or namespace group</li>
 *   <li>{@code "METHOD"}  — a method / function</li>
 *   <li>{@code "CLASS"}   — a class, interface, enum, or other type (default)</li>
 * </ul>
 * Clustering decisions (edge weights, relation types) are driven entirely by
 * these node types — not by which source graph the node originated from.
 */
public class GraphNode {
    private static final String TYPE_PACKAGE = "PACKAGE";
    private static final String TYPE_METHOD  = "METHOD";

    private Long id;
    private String stableKey;
    private String name;
    private String qualifiedName;
    private String type;
    private String packageName;
    private Map<String, Object> metadata = new LinkedHashMap<>();

    public GraphNode() {
    }

    public GraphNode(Long id, String stableKey, String name, String qualifiedName, String type, String packageName) {
        this.id = id;
        this.stableKey = stableKey;
        this.name = name;
        this.qualifiedName = qualifiedName;
        this.type = type;
        this.packageName = packageName;
    }

    // -------------------------------------------------------------------------
    // Type helpers — avoid string literals scattered across the codebase
    // -------------------------------------------------------------------------

    /** Returns {@code true} if this node represents a Java package / namespace group. */
    public boolean isPackage() {
        return TYPE_PACKAGE.equalsIgnoreCase(type);
    }

    /** Returns {@code true} if this node represents a method or function. */
    public boolean isMethod() {
        return TYPE_METHOD.equalsIgnoreCase(type);
    }

    /** Returns {@code true} if this node represents a class, interface, or enum (the default). */
    public boolean isClass() {
        return !isPackage() && !isMethod();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStableKey() {
        return stableKey;
    }

    public void setStableKey(String stableKey) {
        this.stableKey = stableKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}
