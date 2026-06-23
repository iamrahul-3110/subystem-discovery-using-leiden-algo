package com.example.subsystemdiscovery.leiden;

import com.example.subsystemdiscovery.extraction.dto.RawFieldDto;
import com.example.subsystemdiscovery.extraction.dto.RawGraphDto;
import com.example.subsystemdiscovery.extraction.dto.RawLinkDto;
import com.example.subsystemdiscovery.extraction.dto.RawNodeDto;
import com.example.subsystemdiscovery.leiden.model.GraphNode;
import com.example.subsystemdiscovery.leiden.model.RelationType;
import com.example.subsystemdiscovery.leiden.model.WeightedEdge;
import com.example.subsystemdiscovery.leiden.model.WeightedGraph;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Transforms raw graph input ({@link RawGraphDto} list) into a normalised
 * {@link WeightedGraph} ready to be fed into the Leiden algorithm.
 *
 * <p><strong>Design principle:</strong> all clustering decisions (relation type
 * and edge weight) are driven solely by the <em>node types</em>
 * ({@code CLASS}, {@code METHOD}, {@code PACKAGE}). The graph-source type
 * ({@code graphType} on the incoming DTO) is accepted for backward compatibility
 * but is <em>not</em> consulted during processing.
 *
 * <h3>Weight table</h3>
 * <pre>
 *  METHOD  → METHOD   : METHOD_CALL            5.0
 *  METHOD  ↔ CLASS    : CLASS_METHOD_OWNERSHIP  4.5
 *  CLASS   → CLASS    : CLASS_DEPENDENCY        4.0
 *  PACKAGE → *        : PACKAGE_CONTAINMENT     1.5 (direct) / 1.0 (indirect)
 *  *       → *        : UNKNOWN                 1.5
 * </pre>
 */
@Service
public class WeightedGraphBuilder {

    // -------------------------------------------------------------------------
    // Canonical node-type constants
    // -------------------------------------------------------------------------
    private static final String TYPE_PACKAGE = "PACKAGE";
    private static final String TYPE_METHOD  = "METHOD";
    private static final String TYPE_CLASS   = "CLASS";

    // -------------------------------------------------------------------------
    // Edge weights by relation type
    // -------------------------------------------------------------------------
    private static final double W_METHOD_CALL            = 5.0;
    private static final double W_CLASS_METHOD_OWNERSHIP = 4.5;
    private static final double W_CLASS_DEPENDENCY       = 4.0;
    private static final double W_PACKAGE_DIRECT         = 1.5;  // parent directly contains child
    private static final double W_PACKAGE_INDIRECT       = 1.0;  // grandparent+ containment
    private static final double W_UNKNOWN                = 1.5;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link WeightedGraph} from the supplied raw graph DTOs.
     *
     * <p>Nodes are de-duplicated by a stable key derived from their qualified name
     * and type. Edges accumulate weight across multiple occurrences of the same
     * source–target pair.
     *
     * @param rawGraphs one or more raw graph payloads (inline JSON or DB-loaded)
     * @return normalised weighted graph ready for Leiden clustering
     */
    public WeightedGraph build(List<RawGraphDto> rawGraphs) {
        Map<String, GraphNode>   nodeByStableKey = new LinkedHashMap<>();
        Map<String, WeightedEdge> edgeByPair     = new LinkedHashMap<>();

        for (RawGraphDto rawGraph : rawGraphs) {
            List<RawNodeDto> rawNodes = rawGraph.nodeDataArray() == null ? List.of() : rawGraph.nodeDataArray();
            List<RawLinkDto> rawLinks = rawGraph.linkDataArray() == null ? List.of() : rawGraph.linkDataArray();

            // Index raw nodes by their key for quick lookup
            Map<String, RawNodeDto> rawNodeByKey = new HashMap<>(rawNodes.size() * 2);
            for (RawNodeDto rawNode : rawNodes) {
                if (StringUtils.hasText(rawNode.key())) {
                    rawNodeByKey.put(rawNode.key(), rawNode);
                }
            }

            // Normalise and de-duplicate nodes; build raw-key → stable-id map
            Map<String, Long> stableIdByRawKey = new HashMap<>(rawNodes.size() * 2);
            for (RawNodeDto rawNode : rawNodes) {
                NormalizedNode normalized = normalizeNode(rawNode, rawNodeByKey);
                GraphNode graphNode = nodeByStableKey.computeIfAbsent(normalized.stableKey(), key -> new GraphNode(
                        stableLong(key),
                        key,
                        normalized.name(),
                        normalized.qualifiedName(),
                        normalized.type(),
                        normalized.packageName()
                ));
                graphNode.getMetadata().putIfAbsent("displayName", normalized.name());
                if (StringUtils.hasText(rawNode.key())) {
                    stableIdByRawKey.put(rawNode.key(), graphNode.getId());
                }
            }

            // Build explicit edges from link data — weight driven by node types
            for (RawLinkDto rawLink : rawLinks) {
                Long source = stableIdByRawKey.get(rawLink.from());
                Long target = stableIdByRawKey.get(rawLink.to());
                if (source == null || target == null || source.equals(target)) {
                    continue;
                }
                RawNodeDto fromNode = rawNodeByKey.get(rawLink.from());
                RawNodeDto toNode   = rawNodeByKey.get(rawLink.to());
                RelationType relationType = detectRelationType(fromNode, toNode, rawLink);
                addEdge(edgeByPair, source, target, weightFor(relationType, false), relationType);
            }

            // Containment edges from the package/group hierarchy tree.
            // These are deliberately weak so Leiden does not simply rediscover
            // folder structure, but they help isolated nodes join nearby code.
            for (RawNodeDto rawNode : rawNodes) {
                if (!StringUtils.hasText(rawNode.group())) {
                    continue;
                }
                Long child  = stableIdByRawKey.get(rawNode.key());
                Long parent = stableIdByRawKey.get(rawNode.group());
                if (child != null && parent != null && !child.equals(parent)) {
                    addEdge(edgeByPair, parent, child, W_PACKAGE_DIRECT, RelationType.PACKAGE_CONTAINMENT);
                }
            }
        }

        return new WeightedGraph(
                new ArrayList<>(nodeByStableKey.values()),
                edgeByPair.values().stream()
                        .sorted(Comparator.comparing(WeightedEdge::getSource).thenComparing(WeightedEdge::getTarget))
                        .toList()
        );
    }

    // -------------------------------------------------------------------------
    // Edge helpers
    // -------------------------------------------------------------------------

    private void addEdge(Map<String, WeightedEdge> edgeByPair,
                         Long source,
                         Long target,
                         double weight,
                         RelationType relationType) {
        long left  = Math.min(source, target);
        long right = Math.max(source, target);
        String key = left + "--" + right;
        WeightedEdge edge = edgeByPair.computeIfAbsent(key, ignored -> new WeightedEdge(left, right));
        edge.addOccurrence(weight, relationType);
    }

    // -------------------------------------------------------------------------
    // Relation-type detection — purely node-type-driven
    // -------------------------------------------------------------------------

    /**
     * Determines the {@link RelationType} for an edge between two nodes.
     *
     * <p>Decision order:
     * <ol>
     *   <li>If either node is a PACKAGE → {@code PACKAGE_CONTAINMENT}</li>
     *   <li>If both nodes are METHODs → {@code METHOD_CALL}</li>
     *   <li>If one is METHOD and the other is CLASS → {@code CLASS_METHOD_OWNERSHIP}</li>
     *   <li>Optional refinement from {@code rawLink.type} string (calls/extends/implements)</li>
     *   <li>Default CLASS→CLASS → {@code CLASS_DEPENDENCY}</li>
     * </ol>
     */
    private RelationType detectRelationType(RawNodeDto fromNode, RawNodeDto toNode, RawLinkDto rawLink) {
        String fromType = nodeType(fromNode);
        String toType   = nodeType(toNode);

        if (TYPE_PACKAGE.equals(fromType) || TYPE_PACKAGE.equals(toType)) {
            return RelationType.PACKAGE_CONTAINMENT;
        }
        if (TYPE_METHOD.equals(fromType) && TYPE_METHOD.equals(toType)) {
            return RelationType.METHOD_CALL;
        }
        if (TYPE_METHOD.equals(fromType) || TYPE_METHOD.equals(toType)) {
            return RelationType.CLASS_METHOD_OWNERSHIP;
        }

        if (StringUtils.hasText(rawLink.type())) {
            String linkType = rawLink.type().toLowerCase(Locale.ROOT);
            if (linkType.contains("call") || linkType.contains("invoke")) {
                return RelationType.METHOD_CALL;
            }
            if (linkType.contains("extend") || linkType.contains("implement")
                    || linkType.contains("import") || linkType.contains("depend")
                    || linkType.contains("use") || linkType.contains("reference")) {
                return RelationType.CLASS_DEPENDENCY;
            }
        }

        return RelationType.CLASS_DEPENDENCY;
    }

    private double weightFor(RelationType relationType, boolean indirect) {
        return switch (relationType) {
            case METHOD_CALL            -> W_METHOD_CALL;
            case CLASS_METHOD_OWNERSHIP -> W_CLASS_METHOD_OWNERSHIP;
            case CLASS_DEPENDENCY       -> W_CLASS_DEPENDENCY;
            case PACKAGE_CONTAINMENT    -> indirect ? W_PACKAGE_INDIRECT : W_PACKAGE_DIRECT;
            case UNKNOWN                -> W_UNKNOWN;
        };
    }

    // -------------------------------------------------------------------------
    // Node normalisation
    // -------------------------------------------------------------------------

    private NormalizedNode normalizeNode(RawNodeDto rawNode, Map<String, RawNodeDto> rawNodeByKey) {
        String type        = normalizeType(rawNode.type(), rawNode.isGroup());
        String displayName = firstText(rawNode.text(), rawNode.name(), firstFieldName(rawNode), rawNode.key());
        String packageName = resolvePackageName(rawNode, rawNodeByKey);
        String qualifiedName;

        if (TYPE_PACKAGE.equals(type)) {
            qualifiedName = resolvePackagePath(rawNode, rawNodeByKey);
        } else if (StringUtils.hasText(rawNode.packageName())) {
            qualifiedName = rawNode.packageName() + "." + displayName;
        } else if (StringUtils.hasText(packageName)) {
            qualifiedName = packageName + "." + displayName;
        } else {
            qualifiedName = displayName;
        }

        String stableKey = type + ":" + qualifiedName.toLowerCase(Locale.ROOT);
        return new NormalizedNode(stableKey, displayName, qualifiedName, type, packageName);
    }

    private String normalizeType(String rawType, Boolean isGroup) {
        if (StringUtils.hasText(rawType)) {
            String upper = rawType.trim().toUpperCase(Locale.ROOT);
            // Map legacy URI/HTTP types to CLASS — they are treated as code nodes
            if ("URI".equals(upper) || "HTTP_API".equals(upper)) {
                return TYPE_CLASS;
            }
            return upper;
        }
        return Boolean.TRUE.equals(isGroup) ? TYPE_PACKAGE : TYPE_CLASS;
    }

    private String nodeType(RawNodeDto node) {
        if (node == null) {
            return TYPE_CLASS;
        }
        return normalizeType(node.type(), node.isGroup());
    }

    private String resolvePackageName(RawNodeDto rawNode, Map<String, RawNodeDto> rawNodeByKey) {
        if (StringUtils.hasText(rawNode.packageName())) {
            return rawNode.packageName();
        }
        if (!StringUtils.hasText(rawNode.group())) {
            return null;
        }
        RawNodeDto parent = rawNodeByKey.get(rawNode.group());
        if (parent == null) {
            return null;
        }
        return resolvePackagePath(parent, rawNodeByKey);
    }

    private String resolvePackagePath(RawNodeDto rawNode, Map<String, RawNodeDto> rawNodeByKey) {
        ArrayDeque<String> parts = new ArrayDeque<>();
        RawNodeDto current = rawNode;
        int guard = 0;
        while (current != null && guard++ < 200) {
            String part = firstText(current.text(), current.name());
            if (StringUtils.hasText(part)) {
                parts.addFirst(part);
            }
            if (!StringUtils.hasText(current.group())) {
                break;
            }
            current = rawNodeByKey.get(current.group());
        }
        return String.join(".", parts).replace("..", ".");
    }

    private String firstFieldName(RawNodeDto rawNode) {
        return Optional.ofNullable(rawNode.fields())
                .orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(RawFieldDto::name)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }

    private long stableLong(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            long result = 0L;
            for (int i = 0; i < 8; i++) {
                result = (result << 8) | (bytes[i] & 0xffL);
            }
            return result & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    // -------------------------------------------------------------------------
    // Internal record
    // -------------------------------------------------------------------------

    private record NormalizedNode(
            String stableKey,
            String name,
            String qualifiedName,
            String type,
            String packageName
    ) {
    }
}
