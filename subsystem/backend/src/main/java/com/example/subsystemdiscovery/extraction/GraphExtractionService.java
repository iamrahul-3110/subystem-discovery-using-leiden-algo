package com.example.subsystemdiscovery.extraction;

import com.example.subsystemdiscovery.extraction.dto.RawGraphDto;
import com.example.subsystemdiscovery.extraction.dto.RawLinkDto;
import com.example.subsystemdiscovery.extraction.dto.RawNodeDto;
import com.example.subsystemdiscovery.repository.entity.NodeDetailRow;
import com.example.subsystemdiscovery.repository.entity.NodeRow;
import com.example.subsystemdiscovery.repository.TbNodeHistoryMapper;
import com.example.subsystemdiscovery.leiden.model.GraphType;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GraphExtractionService {

    private final TbNodeHistoryMapper tbNodeHistoryMapper;

    public GraphExtractionService(TbNodeHistoryMapper tbNodeHistoryMapper) {
        this.tbNodeHistoryMapper = tbNodeHistoryMapper;
    }

    public List<RawGraphDto> extract(Long applicationId,
                                     String analysisTime,
                                     String applicationKey,
                                     List<GraphType> graphTypes,
                                     List<RawGraphDto> inlineGraphs) {

        // Mode 1 — inline JSON (transform endpoint / API 4)
        if (!CollectionUtils.isEmpty(inlineGraphs)) {
            return inlineGraphs.stream()
                    .filter(Objects::nonNull)
                    .toList();
        }

        // Mode 2 — history tables
        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "applicationId is required when inline graph JSON is not provided");
        }
        if (!StringUtils.hasText(analysisTime)) {
            throw new IllegalArgumentException(
                    "analysisTime is required when inline graph JSON is not provided");
        }

        // Load all graph types by default — node types drive clustering, not graph types.
        List<GraphType> effectiveTypes = CollectionUtils.isEmpty(graphTypes)
                ? Arrays.asList(GraphType.values())
                : graphTypes;

        List<RawGraphDto> graphs = loadFromHistoryTables(applicationId, analysisTime, effectiveTypes);

        if (graphs.isEmpty()) {
            throw new IllegalStateException(
                    "No graph data found in history tables for applicationId=" + applicationId
                    + ", analysisTime=" + analysisTime
                    + (StringUtils.hasText(applicationKey) ? ", applicationKey=" + applicationKey : ""));
        }
        return graphs;
    }

    private List<RawGraphDto> loadFromHistoryTables(Long applicationId,
                                                     String analysisTime,
                                                     List<GraphType> graphTypes) {
        boolean includeCallGraph = graphTypes.contains(GraphType.CALL_GRAPH)
                || graphTypes.contains(GraphType.CODE_ASSOCIATION);

        if (!includeCallGraph) {
            return List.of();
        }

        // 1. Load all node rows for the snapshot
        List<NodeRow> nodeRows = tbNodeHistoryMapper.selectNodes(applicationId, analysisTime);
        if (nodeRows.isEmpty()) {
            return List.of();
        }

        // 2. Load node details and group by nodeId for fast lookup
        Map<String, List<NodeDetailRow>> detailsByNodeId = tbNodeHistoryMapper
                .selectNodeDetails(applicationId, analysisTime)
                .stream()
                .collect(Collectors.groupingBy(
                        NodeDetailRow::nodeId,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        // 3. Build the synthetic node hierarchy: PACKAGE → CLASS → METHOD
        Map<String, RawNodeDto> nodesByKey = new LinkedHashMap<>();
        for (NodeRow nodeRow : nodeRows) {
            MethodParts parts = MethodParts.from(
                    nodeRow, detailsByNodeId.getOrDefault(nodeRow.nodeId(), List.of()));
            addPackageHierarchy(nodesByKey, parts.packageName());
            addClassNode(nodesByKey, parts);
            addMethodNode(nodesByKey, nodeRow.nodeId(), parts);
        }

        // 4. Load edges (call-graph relations)
        List<RawLinkDto> links = tbNodeHistoryMapper.selectLinks(applicationId, analysisTime)
                .stream()
                .map(link -> new RawLinkDto(
                        methodKey(link.fromKey()),
                        methodKey(link.toKey()),
                        null,
                        null,
                        "DB_NODE_RELATION_HISTORY"
                ))
                .toList();

        if (nodesByKey.isEmpty()) {
            return List.of();
        }
        return List.of(new RawGraphDto(GraphType.CALL_GRAPH, new ArrayList<>(nodesByKey.values()), links));
    }

    // -------------------------------------------------------------------------
    // Node-hierarchy builders
    // -------------------------------------------------------------------------

    private void addPackageHierarchy(Map<String, RawNodeDto> nodesByKey, String packageName) {
        if (!StringUtils.hasText(packageName)) {
            return;
        }
        String[] segments = packageName.split("\\.");
        String parentKey = null;
        String currentPackage = "";
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            currentPackage = currentPackage.isEmpty() ? segment : currentPackage + "." + segment;
            String key = packageKey(currentPackage);
            nodesByKey.putIfAbsent(key, new RawNodeDto(
                    key,
                    segment,
                    segment,
                    "PACKAGE",
                    parentKey == null ? null : currentPackage.substring(0, currentPackage.lastIndexOf('.')),
                    parentKey,
                    true,
                    List.of()
            ));
            parentKey = key;
        }
    }

    private void addClassNode(Map<String, RawNodeDto> nodesByKey, MethodParts parts) {
        String classKey = classKey(parts.qualifiedClassName());
        nodesByKey.putIfAbsent(classKey, new RawNodeDto(
                classKey,
                parts.className(),
                parts.className(),
                "CLASS",
                parts.packageName(),
                packageKey(parts.packageName()),
                false,
                List.of()
        ));
    }

    private void addMethodNode(Map<String, RawNodeDto> nodesByKey, String nodeId, MethodParts parts) {
        nodesByKey.putIfAbsent(methodKey(nodeId), new RawNodeDto(
                methodKey(nodeId),
                parts.methodDisplayName(),
                parts.methodDisplayName(),
                "METHOD",
                parts.packageName(),
                classKey(parts.qualifiedClassName()),
                false,
                List.of()
        ));
    }

    // -------------------------------------------------------------------------
    // Key helpers
    // -------------------------------------------------------------------------

    private static String methodKey(String nodeId) {
        return "method:" + nodeId;
    }

    private static String classKey(String qualifiedClassName) {
        return "class:" + sanitizeKey(qualifiedClassName);
    }

    private static String packageKey(String packageName) {
        return StringUtils.hasText(packageName) ? "package:" + sanitizeKey(packageName) : null;
    }

    private static String sanitizeKey(String value) {
        return Objects.requireNonNullElse(value, "unknown").trim().toLowerCase(Locale.ROOT);
    }

    // -------------------------------------------------------------------------
    // Inner types — name parsing
    // -------------------------------------------------------------------------

    /**
     * Holds the decomposed name parts (package, class, method) for one node row.
     */
    private record MethodParts(String packageName, String className, String methodName,
                                String qualifiedClassName) {

        private String methodDisplayName() {
            return className + "." + methodName;
        }

        private static MethodParts from(NodeRow row, List<NodeDetailRow> details) {
            List<NodeDetailRow> sorted = details.stream()
                    .sorted(Comparator.comparingInt(NodeDetailRow::splitNodeLevel))
                    .toList();

            String methodName  = firstDetailName(sorted, "METHOD",  0);
            String className   = firstDetailName(sorted, "CLASS",   1);
            String packageName = firstDetailName(sorted, "PACKAGE", 2);

            ParsedNodeName parsed = ParsedNodeName.from(row.nodeName());
            methodName  = firstText(methodName,  parsed.methodName(),  row.nodeName(), row.nodeId());
            className   = firstText(className,   parsed.className(),   "UnknownClass");
            packageName = firstText(packageName, parsed.packageName(), "unknown");

            return new MethodParts(packageName, className, methodName, packageName + "." + className);
        }

        private static String firstDetailName(List<NodeDetailRow> details, String type, int level) {
            return details.stream()
                    .filter(d -> type.equalsIgnoreCase(d.splitNodeType()) || d.splitNodeLevel() == level)
                    .map(NodeDetailRow::splitNodeName)
                    .filter(StringUtils::hasText)
                    .findFirst()
                    .orElse(null);
        }
    }

    /**
     * Parses a fully-qualified method name string into package, class, and method components.
     */
    private record ParsedNodeName(String packageName, String className, String methodName) {

        private static ParsedNodeName from(String value) {
            if (!StringUtils.hasText(value)) {
                return new ParsedNodeName(null, null, null);
            }
            String trimmed          = value.trim();
            int paramStart          = trimmed.indexOf('(');
            String beforeParams     = paramStart >= 0 ? trimmed.substring(0, paramStart) : trimmed;
            int methodSep           = beforeParams.lastIndexOf('.');
            if (methodSep < 0) {
                return new ParsedNodeName(null, null, trimmed);
            }
            String methodName         = trimmed.substring(methodSep + 1);
            String qualifiedClassName = beforeParams.substring(0, methodSep);
            int classSep              = qualifiedClassName.lastIndexOf('.');
            if (classSep < 0) {
                return new ParsedNodeName(null, qualifiedClassName, methodName);
            }
            return new ParsedNodeName(
                    qualifiedClassName.substring(0, classSep),
                    qualifiedClassName.substring(classSep + 1),
                    methodName
            );
        }
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return "unknown";
    }
}
