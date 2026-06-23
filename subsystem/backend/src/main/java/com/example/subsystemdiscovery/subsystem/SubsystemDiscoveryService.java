package com.example.subsystemdiscovery.subsystem;
import com.example.subsystemdiscovery.llm.LlmSubsystemDiscoveryService;

import com.example.subsystemdiscovery.subsystem.dto.AlgorithmInfoDto;
import com.example.subsystemdiscovery.llm.dto.LlmDiscoveryResponse;
import com.example.subsystemdiscovery.llm.dto.LlmSummaryInput;
import com.example.subsystemdiscovery.subsystem.dto.SummaryType;
import com.example.subsystemdiscovery.extraction.dto.RawGraphDto;
import com.example.subsystemdiscovery.leiden.dto.LeidenInputDto;
import com.example.subsystemdiscovery.leiden.dto.LeidenInputEdgeDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemAlgorithmParams;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDiscoveryResponse;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemPersistenceDto;
import com.example.subsystemdiscovery.subsystem.dto.SummaryDto;
import com.example.subsystemdiscovery.repository.entity.ApplicationMetadata;
import com.example.subsystemdiscovery.repository.entity.SubsystemRunMaster;
import com.example.subsystemdiscovery.repository.SubsystemHistoryMapper;
import com.example.subsystemdiscovery.repository.TbNodeHistoryMapper;
import com.example.subsystemdiscovery.extraction.GraphExtractionService;
import com.example.subsystemdiscovery.leiden.model.WeightedGraph;
import com.example.subsystemdiscovery.leiden.WeightedGraphBuilder;
import com.example.subsystemdiscovery.leiden.model.ClusterAggregation;
import com.example.subsystemdiscovery.leiden.model.LeidenClusteringResult;
import com.example.subsystemdiscovery.leiden.model.LabelResult;
import com.example.subsystemdiscovery.leiden.model.SubsystemDraft;
import com.example.subsystemdiscovery.leiden.ClusterAggregationUtil;
import com.example.subsystemdiscovery.leiden.LeidenAlgorithmUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Orchestrates the full subsystem-discovery pipeline.
 *
 * <p>All data is read from the <strong>history tables</strong>
 * ({@code tb_node_history}, {@code tb_node_detail_history},
 * {@code tb_node_relation_history}) via {@link GraphExtractionService}.
 * If the application snapshot credentials are not provided in the parameters,
 * they are resolved dynamically from the {@code analysisTime} parameter
 * using the metadata lookup mappings.
 */
@Service
public class SubsystemDiscoveryService {

    private static final String WEIGHTING_VERSION =
            "v2-method5-classmethod4.5-class4-pkg1.5-nodecentric-adaptive";

    private final GraphExtractionService graphExtractionService;
    private final WeightedGraphBuilder weightedGraphBuilder;
    private final LeidenAlgorithmUtil leidenAlgorithmUtil;
    private final ClusterAggregationUtil clusterAggregationUtil;
    private final SubsystemLabelService subsystemLabelService;
    private final LlmSubsystemDiscoveryService llmSubsystemDiscoveryService;
    private final TbNodeHistoryMapper tbNodeHistoryMapper;
    private final SubsystemHistoryMapper subsystemHistoryMapper;
    private final ObjectMapper objectMapper;

    public SubsystemDiscoveryService(GraphExtractionService graphExtractionService,
                                     WeightedGraphBuilder weightedGraphBuilder,
                                     LeidenAlgorithmUtil leidenAlgorithmUtil,
                                     ClusterAggregationUtil clusterAggregationUtil,
                                     SubsystemLabelService subsystemLabelService,
                                     LlmSubsystemDiscoveryService llmSubsystemDiscoveryService,
                                     TbNodeHistoryMapper tbNodeHistoryMapper,
                                     SubsystemHistoryMapper subsystemHistoryMapper,
                                     ObjectMapper objectMapper) {
        this.graphExtractionService       = graphExtractionService;
        this.weightedGraphBuilder         = weightedGraphBuilder;
        this.leidenAlgorithmUtil          = leidenAlgorithmUtil;
        this.clusterAggregationUtil       = clusterAggregationUtil;
        this.subsystemLabelService        = subsystemLabelService;
        this.llmSubsystemDiscoveryService = llmSubsystemDiscoveryService;
        this.tbNodeHistoryMapper          = tbNodeHistoryMapper;
        this.subsystemHistoryMapper       = subsystemHistoryMapper;
        this.objectMapper                 = objectMapper;
    }

    public LeidenInputDto toLeidenInput(String analysisTime,
                                        SubsystemAlgorithmParams params) {
        ApplicationMetadata meta = resolveMetadata(null, null, analysisTime);
        Long resolvedId = meta.applicationId();
        String resolvedKey = meta.applicationKey();

        List<RawGraphDto> rawGraphs = collectGraphs(resolvedId, analysisTime, resolvedKey, params);
        WeightedGraph weightedGraph = weightedGraphBuilder.build(rawGraphs);
        return new LeidenInputDto(
                weightedGraph.getNodes().size(),
                weightedGraph.getEdges().size(),
                weightedGraph.getNodes().stream().map(node -> node.getId()).toList(),
                weightedGraph.getEdges().stream()
                        .map(edge -> new LeidenInputEdgeDto(edge.getSource(), edge.getTarget(), edge.getWeight()))
                        .toList()
        );
    }

    @Transactional
    public SubsystemDiscoveryResponse discover(String analysisTime,
                                               SubsystemAlgorithmParams params) {
        ApplicationMetadata meta = resolveMetadata(null, null, analysisTime);
        Long resolvedId = meta.applicationId();
        String resolvedKey = meta.applicationKey();

        int    runs               = params.runsOrDefault();
        double consensusThreshold = params.consensusThresholdOrDefault();
        double resolution         = params.resolutionOrDefault();

        // 1. Check if a run matching the parameters already exists in the database
        SubsystemRunMaster existingMaster = subsystemHistoryMapper.selectMasterByConfig(
                analysisTime, runs, consensusThreshold, resolution);

        if (existingMaster != null) {
            String cachedResult = subsystemHistoryMapper.selectHistoryResult(existingMaster.getDiscoveryRunId());
            if (StringUtils.hasText(cachedResult)) {
                try {
                    SubsystemPersistenceDto persisted = objectMapper.readValue(
                            cachedResult, SubsystemPersistenceDto.class);

                    int totalNodes = persisted.subsystems().stream()
                            .mapToInt(SubsystemDto::nodeCount)
                            .sum();
                    int totalEdges = persisted.subsystems().stream()
                            .mapToInt(SubsystemDto::edgeCount)
                            .sum()
                            + persisted.subsystemLinks().stream()
                            .mapToInt(SubsystemLinkDto::edgeCount)
                            .sum();

                    SummaryDto summary = new SummaryDto(
                            totalNodes,
                            totalEdges,
                            existingMaster.getTotalSubsystems(),
                            existingMaster.getAvgStabilityScore()
                    );

                    AlgorithmInfoDto algorithm = new AlgorithmInfoDto(
                            "Leiden (Java)",
                            existingMaster.getRuns(),
                            existingMaster.getConsensusThreshold(),
                            existingMaster.getResolution(),
                            WEIGHTING_VERSION
                    );

                    // Reconstruct response with active run ID just to ensure consistency
                    return new SubsystemDiscoveryResponse(
                            existingMaster.getDiscoveryRunId(),
                            resolvedId,
                            resolvedKey,
                            algorithm,
                            summary,
                            persisted.subsystems(),
                            persisted.subsystemLinks(),
                            null
                    );
                } catch (Exception e) {
                    // Fall back to executing Leiden if deserialization fails
                }
            }
        }

        LocalDateTime startedAt = LocalDateTime.now(ZoneId.systemDefault());

        List<RawGraphDto> rawGraphs   = collectGraphs(resolvedId, analysisTime, resolvedKey, params);
        WeightedGraph weightedGraph   = weightedGraphBuilder.build(rawGraphs);
        if (weightedGraph.getNodes().isEmpty()) {
            throw new IllegalStateException("Cannot discover subsystems from an empty graph");
        }

        LeidenClusteringResult clusteringResult = leidenAlgorithmUtil.cluster(
                weightedGraph, runs, consensusThreshold, resolution);
        ClusterAggregation aggregation = clusterAggregationUtil.aggregate(weightedGraph, clusteringResult);

        // Labels always come from the heuristic path.
        List<SubsystemDto> subsystems = aggregation.getSubsystemDrafts().stream()
                .map(this::toSubsystemDto)
                .toList();

        double averageStability = subsystems.stream()
                .mapToDouble(SubsystemDto::stabilityScore)
                .average()
                .orElse(1.0);

        LocalDateTime completedAt = LocalDateTime.now(ZoneId.systemDefault());

        SubsystemDiscoveryResponse response = new SubsystemDiscoveryResponse(
                null, // discoveryRunId will be populated after inserting into the table
                resolvedId,
                resolvedKey,
                new AlgorithmInfoDto("Leiden (Java)", runs, consensusThreshold, resolution, WEIGHTING_VERSION),
                new SummaryDto(weightedGraph.getNodes().size(), weightedGraph.getEdges().size(),
                                subsystems.size(), round(averageStability)),
                subsystems,
                aggregation.getSubsystemLinks(),
                aggregation.getNodeAssignments()
        );

        // Persist the new run results to database
        try {
            SubsystemRunMaster master = new SubsystemRunMaster();
            master.setAnalysisTime(analysisTime);
            master.setRuns(runs);
            master.setConsensusThreshold(consensusThreshold);
            master.setResolution(resolution);
            master.setStartedAt(startedAt);
            master.setCompletedAt(completedAt);
            master.setTotalSubsystems(subsystems.size());
            master.setAvgStabilityScore(round(averageStability));
            SubsystemPersistenceDto persistenceDto = new SubsystemPersistenceDto(
                    response.subsystems(), response.subsystemLinks());
            master.setDiscoveryResult(objectMapper.writeValueAsString(persistenceDto));
            master.setCreatedByEnvelope("dummy-envelope".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            master.setCreatedByHash("dummy-hash".getBytes(java.nio.charset.StandardCharsets.UTF_8));

            subsystemHistoryMapper.insertDiscovery(master);

            Long runId = master.getDiscoveryRunId();

            // Populate the response with the generated run ID
            response = new SubsystemDiscoveryResponse(
                    runId,
                    resolvedId,
                    resolvedKey,
                    response.algorithm(),
                    response.summary(),
                    response.subsystems(),
                    response.subsystemLinks(),
                    response.nodeAssignments()
            );

        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist discovery run results to the database", e);
        }

        return response;
    }

    @Transactional
    public LlmDiscoveryResponse discoverWithLlm(String analysisTime,
                                                SubsystemAlgorithmParams params) {
        ApplicationMetadata meta = resolveMetadata(null, null, analysisTime);
        Long resolvedId = meta.applicationId();
        String resolvedKey = meta.applicationKey();

        int    runs               = params.runsOrDefault();
        double consensusThreshold = params.consensusThresholdOrDefault();
        double resolution         = params.resolutionOrDefault();

        String modelId = params.llmModel() != null ? params.llmModel() : "43";
        SummaryType resolvedSummaryType = params.summaryTypeOrDefault();
        String summaryTypeStr = resolvedSummaryType.name();

        // 1. Check if a run matching these configurations already exists
        SubsystemRunMaster master = subsystemHistoryMapper.selectMasterByConfig(
                analysisTime, runs, consensusThreshold, resolution);

        if (master == null) {
            // Run Leiden to create a new master run
            discover(analysisTime, params);
            // After discover(), a row is inserted in tb_gi_subsystems_history. Let's load it.
            master = subsystemHistoryMapper.selectMasterByConfig(
                    analysisTime, runs, consensusThreshold, resolution);
            if (master == null) {
                throw new IllegalStateException("Failed to retrieve master run after execution");
            }
        }

        // 2. Deserialize discovery result
        SubsystemPersistenceDto persisted;
        try {
            String cachedResult = master.getDiscoveryResult();
            if (!StringUtils.hasText(cachedResult)) {
                cachedResult = subsystemHistoryMapper.selectHistoryResult(master.getDiscoveryRunId());
            }
            persisted = objectMapper.readValue(cachedResult, SubsystemPersistenceDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize discovery result for run ID: " + master.getDiscoveryRunId(), e);
        }

        // 3. Convert to LlmSummaryInput
        LlmSummaryInput llmInput = new LlmSummaryInput(
                master.getTotalSubsystems(),
                master.getAvgStabilityScore(),
                persisted.subsystems(),
                persisted.subsystemLinks()
        );

        // 4. Check if matching summary already exists
        String cachedSummary = subsystemHistoryMapper.selectLlmSummaryByConfig(
                master.getDiscoveryRunId(), modelId, summaryTypeStr);

        if (!StringUtils.hasText(cachedSummary)) {
            cachedSummary = llmSubsystemDiscoveryService.summarise(llmInput, resolvedSummaryType, modelId);
            try {
                subsystemHistoryMapper.insertLlmSummary(
                        master.getDiscoveryRunId(),
                        modelId,
                        summaryTypeStr,
                        cachedSummary
                );
            } catch (Exception e) {
                throw new IllegalStateException("Failed to persist LLM architectural summary to the database", e);
            }
        }

        // 5. Build response DTOs
        int totalNodes = persisted.subsystems().stream()
                .mapToInt(SubsystemDto::nodeCount)
                .sum();
        int totalEdges = persisted.subsystems().stream()
                .mapToInt(SubsystemDto::edgeCount)
                .sum()
                + persisted.subsystemLinks().stream()
                .mapToInt(SubsystemLinkDto::edgeCount)
                .sum();

        SummaryDto summary = new SummaryDto(
                totalNodes,
                totalEdges,
                master.getTotalSubsystems(),
                master.getAvgStabilityScore()
        );

        AlgorithmInfoDto algorithm = new AlgorithmInfoDto(
                "Leiden (Java)",
                master.getRuns(),
                master.getConsensusThreshold(),
                master.getResolution(),
                WEIGHTING_VERSION
        );

        return new LlmDiscoveryResponse(
                master.getDiscoveryRunId(),
                resolvedId,
                resolvedKey,
                analysisTime,
                algorithm,
                summary,
                persisted.subsystems(),
                persisted.subsystemLinks(),
                null,
                cachedSummary
        );
    }

    @Transactional
    public String generateSummary(Long discoveryRunId, String llmModel, SummaryType summaryType) {
        // 1. Fetch the existing master run record
        SubsystemRunMaster master = subsystemHistoryMapper.selectMasterById(discoveryRunId);
        if (master == null) {
            throw new IllegalArgumentException("No discovery run found for ID: " + discoveryRunId);
        }

        String modelId = llmModel != null && !llmModel.trim().isEmpty() ? llmModel : "43";
        SummaryType resolvedSummaryType = summaryType != null ? summaryType : SummaryType.MEDIUM_DETAILED;
        String summaryTypeStr = resolvedSummaryType.name();

        // 2. Check cache first
        String cachedSummary = subsystemHistoryMapper.selectLlmSummaryByConfig(
                discoveryRunId, modelId, summaryTypeStr);

        if (StringUtils.hasText(cachedSummary)) {
            return cachedSummary;
        }

        // 3. Deserialize discovery result
        SubsystemPersistenceDto persisted;
        try {
            String cachedResult = master.getDiscoveryResult();
            if (!StringUtils.hasText(cachedResult)) {
                cachedResult = subsystemHistoryMapper.selectHistoryResult(discoveryRunId);
            }
            if (!StringUtils.hasText(cachedResult)) {
                throw new IllegalStateException("No discovery result data found for run ID: " + discoveryRunId);
            }
            persisted = objectMapper.readValue(cachedResult, SubsystemPersistenceDto.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize discovery result for run ID: " + discoveryRunId, e);
        }

        // 4. Convert to LlmSummaryInput
        LlmSummaryInput llmInput = new LlmSummaryInput(
                master.getTotalSubsystems(),
                master.getAvgStabilityScore(),
                persisted.subsystems(),
                persisted.subsystemLinks()
        );

        // 5. Generate summary
        cachedSummary = llmSubsystemDiscoveryService.summarise(llmInput, resolvedSummaryType, modelId);
        try {
            subsystemHistoryMapper.insertLlmSummary(
                    discoveryRunId,
                    modelId,
                    summaryTypeStr,
                    cachedSummary
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to persist LLM architectural summary to the database", e);
        }

        return cachedSummary;
    }

    private ApplicationMetadata resolveMetadata(Long applicationId, String applicationKey, String analysisTime) {
        Long resolvedId = applicationId;
        String resolvedKey = applicationKey;
        if (resolvedId == null || !StringUtils.hasText(resolvedKey)) {
            ApplicationMetadata meta = tbNodeHistoryMapper.selectApplicationMetadata(analysisTime);
            if (meta == null) {
                throw new IllegalArgumentException(
                        "Cannot resolve application metadata for analysisTime=" + analysisTime);
            }
            if (resolvedId == null) {
                resolvedId = meta.applicationId();
            }
            if (!StringUtils.hasText(resolvedKey)) {
                resolvedKey = meta.applicationKey();
            }
        }
        return new ApplicationMetadata(resolvedId, resolvedKey);
    }

    private List<RawGraphDto> collectGraphs(Long applicationId,
                                            String analysisTime,
                                            String applicationKey,
                                            SubsystemAlgorithmParams params) {
        return graphExtractionService.extract(
                applicationId,
                analysisTime,
                applicationKey,
                params.graphTypes(),
                params.graphs()
        );
    }

    private SubsystemDto toSubsystemDto(SubsystemDraft draft) {
        LabelResult label = subsystemLabelService.label(draft);
        return new SubsystemDto(
                draft.getId(),
                label.name(),
                label.description(),
                draft.getStabilityScore(),
                draft.getNodeCount(),
                draft.getEdgeCount(),
                draft.getInternalConnectivity(),
                draft.getTopPackages(),
                draft.getCentralNodes(),
                draft.getApiEndpoints(),
                draft.getRelationSummary() == null ? java.util.Map.of() : draft.getRelationSummary()
        );
    }

    private static double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
