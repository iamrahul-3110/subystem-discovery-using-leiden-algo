package com.example.subsystemdiscovery.api;

import com.example.subsystemdiscovery.discovery.dto.SubsystemAlgorithmParams;
import com.example.subsystemdiscovery.discovery.dto.SubsystemDiscoveryResponse;
import com.example.subsystemdiscovery.discovery.dto.SummaryType;
import com.example.subsystemdiscovery.repository.SubsystemHistoryMapper;
import com.example.subsystemdiscovery.dataset.DomainTemplate;
import com.example.subsystemdiscovery.dataset.SyntheticDataGenerator;
import com.example.subsystemdiscovery.dataset.SyntheticDataGenerator.GeneratedDataset;
import com.example.subsystemdiscovery.llm.LlmProvider;
import com.example.subsystemdiscovery.llm.LlmProviderFactory;
import com.example.subsystemdiscovery.visualization.MermaidGenerator;
import com.example.subsystemdiscovery.discovery.SubsystemDiscoveryService;
import com.example.subsystemdiscovery.visualization.SummaryFormatterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/poc")
@CrossOrigin(origins = "*")
public class PocController {

    private static final Logger log = LoggerFactory.getLogger(PocController.class);

    private final SyntheticDataGenerator dataGenerator;
    private final SubsystemDiscoveryService discoveryService;
    private final MermaidGenerator mermaidGenerator;
    private final LlmProviderFactory llmProviderFactory;
    private final SubsystemHistoryMapper historyMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;
    private final SummaryFormatterService formatterService;

    public PocController(SyntheticDataGenerator dataGenerator,
                         SubsystemDiscoveryService discoveryService,
                         MermaidGenerator mermaidGenerator,
                         LlmProviderFactory llmProviderFactory,
                         SubsystemHistoryMapper historyMapper,
                         ObjectMapper objectMapper,
                         JdbcTemplate jdbcTemplate,
                         SummaryFormatterService formatterService) {
        this.dataGenerator = dataGenerator;
        this.discoveryService = discoveryService;
        this.mermaidGenerator = mermaidGenerator;
        this.llmProviderFactory = llmProviderFactory;
        this.historyMapper = historyMapper;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;
        this.formatterService = formatterService;
    }

    @PostMapping("/dataset/generate")
    public ResponseEntity<?> generateDataset(@RequestParam DomainTemplate template,
                                             @RequestParam int nodeCount) {
        log.info("Generating POC dataset template={}, nodeCount={}", template, nodeCount);
        try {
            GeneratedDataset dataset = dataGenerator.generateDataset(template, nodeCount);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("applicationId", dataset.applicationId());
            body.put("applicationKey", dataset.applicationKey());
            body.put("applicationName", dataset.applicationName());
            body.put("analysisTime", dataset.analysisTime());
            body.put("template", dataset.template());
            body.put("nodeCount", dataset.nodeCount());
            body.put("relationCount", dataset.relationCount());
            body.put("message", "Dataset generated successfully");
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Dataset generation failed", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @PostMapping("/discover")
    public ResponseEntity<?> discover(@RequestParam Long applicationId,
                                      @RequestParam String applicationKey,
                                      @RequestParam String analysisTime,
                                      @RequestParam(defaultValue = "10") int runs,
                                      @RequestParam(defaultValue = "0.7") double consensusThreshold,
                                      @RequestParam(defaultValue = "1.0") double resolution) {
        log.info("Running POC discovery applicationId={}, analysisTime={}, runs={}, threshold={}, resolution={}",
                applicationId, analysisTime, runs, consensusThreshold, resolution);
        try {
            int effectiveRuns = capRuns(applicationId, analysisTime, runs);
            SubsystemAlgorithmParams params = new SubsystemAlgorithmParams(
                    null,
                    effectiveRuns,
                    consensusThreshold,
                    resolution,
                    null,
                    SummaryType.MEDIUM_DETAILED,
                    null
            );
            SubsystemDiscoveryResponse response = discoveryService.discover(
                    analysisTime, params);
            log.info("Subsystem discovery completed successfully for applicationId={}. Found {} subsystems.",
                    applicationId, response.subsystems().size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Subsystem discovery failed", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @PostMapping("/summary")
    public ResponseEntity<?> generateSummary(@RequestParam Long applicationId,
                                             @RequestParam String applicationKey,
                                             @RequestParam String analysisTime,
                                             @RequestParam(defaultValue = "10") int runs,
                                             @RequestParam(defaultValue = "0.7") double consensusThreshold,
                                             @RequestParam(defaultValue = "1.0") double resolution,
                                             @RequestParam(defaultValue = "43") String llmModel,
                                             @RequestParam(defaultValue = "MEDIUM_DETAILED") String summaryType) {
        log.info("Generating POC summary applicationId={}, analysisTime={}, model={}, type={}",
                applicationId, analysisTime, llmModel, summaryType);
        try {
            int effectiveRuns = capRuns(applicationId, analysisTime, runs);
            SummaryType resolvedSummaryType = parseSummaryType(summaryType);
            SubsystemAlgorithmParams params = new SubsystemAlgorithmParams(
                    null,
                    effectiveRuns,
                    consensusThreshold,
                    resolution,
                    llmModel,
                    resolvedSummaryType,
                    null
            );
            SubsystemDiscoveryResponse response = discoveryService.discover(
                    analysisTime, params);

            boolean fallback = false;
            String providerName = "MOCK".equalsIgnoreCase(llmModel) ? "MOCK" : "REAL";
            String summary;
            try {
                LlmProvider provider = llmProviderFactory.getProvider(providerName);
                summary = provider.generateSummary(response, resolvedSummaryType.name(), llmModel);
            } catch (Exception realFailure) {
                fallback = true;
                providerName = "MOCK";
                log.warn("Real LLM summary unavailable; falling back to mock provider: {}", realFailure.getMessage());
                summary = llmProviderFactory.getProvider("MOCK")
                        .generateSummary(response, resolvedSummaryType.name(), llmModel);
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("summary", summary);
            body.put("formattedSummary", formatterService.format(summary));
            body.put("provider", providerName);
            body.put("fallback", fallback);
            body.put("llmModel", llmModel);
            body.put("summaryType", resolvedSummaryType.name());
            body.put("discoveryRunId", response.discoveryRunId());
            log.info("Summary successfully generated for application key='{}' using provider '{}' (fallback={}). Summary length={}",
                    applicationKey, providerName, fallback, summary.length());
            return ResponseEntity.ok(body);
        } catch (Exception e) {
            log.error("Summary generation failed", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    @GetMapping("/visualize/mermaid")
    public ResponseEntity<?> getMermaidDiagram(@RequestParam Long discoveryRunId) {
        log.info("Request received to generate Mermaid diagram for discoveryRunId={}", discoveryRunId);
        try {
            String cached = historyMapper.selectHistoryResult(discoveryRunId);
            if (!StringUtils.hasText(cached)) {
                log.warn("No discovery result found in history cache for discoveryRunId={}", discoveryRunId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "No discovery result found for discoveryRunId=" + discoveryRunId));
            }
            SubsystemDiscoveryResponse response = objectMapper.readValue(cached, SubsystemDiscoveryResponse.class);
            log.info("Loaded cached discovery result for application '{}' (discoveryRunId={})", response.applicationKey(), discoveryRunId);
            return ResponseEntity.ok(mermaidGenerator.generate(response.subsystemLinks(), response.subsystems()));
        } catch (Exception e) {
            log.error("Mermaid generation failed", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, e);
        }
    }

    private int capRuns(Long applicationId, String analysisTime, int requestedRuns) {
        int safeRuns = Math.max(1, requestedRuns);
        Integer nodeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                  FROM tb_node_history
                 WHERE application_id = ?
                   AND analysis_time = CAST(? AS TIMESTAMP)
                """, Integer.class, applicationId, analysisTime);
        int nodes = nodeCount == null ? 0 : nodeCount;
        if (nodes >= 50_000) {
            return Math.min(safeRuns, 1);
        }
        if (nodes >= 10_000) {
            return Math.min(safeRuns, 5);
        }
        return safeRuns;
    }

    private SummaryType parseSummaryType(String value) {
        if (!StringUtils.hasText(value)) {
            return SummaryType.MEDIUM_DETAILED;
        }
        return switch (value.toUpperCase()) {
            case "SMALL", "LESS", "LESS_DETAILED" -> SummaryType.LESS_DETAILED;
            case "LARGE", "COMPLETE", "COMPLETE_DETAILED" -> SummaryType.COMPLETE_DETAILED;
            default -> SummaryType.MEDIUM_DETAILED;
        };
    }

    private ResponseEntity<Map<String, String>> error(HttpStatus status, Exception e) {
        return ResponseEntity.status(status)
                .body(Map.of("error", e.getMessage() == null ? "Unknown error" : e.getMessage()));
    }
}
