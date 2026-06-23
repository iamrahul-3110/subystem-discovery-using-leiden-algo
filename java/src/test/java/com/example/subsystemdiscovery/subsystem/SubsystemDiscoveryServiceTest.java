package com.example.subsystemdiscovery.subsystem;
import com.example.subsystemdiscovery.llm.LlmSubsystemDiscoveryService;

import com.example.subsystemdiscovery.llm.dto.LlmDiscoveryResponse;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemLinkDto;
import com.example.subsystemdiscovery.subsystem.dto.SubsystemPersistenceDto;
import com.example.subsystemdiscovery.subsystem.dto.SummaryType;
import com.example.subsystemdiscovery.repository.entity.ApplicationMetadata;
import com.example.subsystemdiscovery.repository.entity.SubsystemRunMaster;
import com.example.subsystemdiscovery.repository.SubsystemHistoryMapper;
import com.example.subsystemdiscovery.repository.TbNodeHistoryMapper;
import com.example.subsystemdiscovery.extraction.GraphExtractionService;
import com.example.subsystemdiscovery.leiden.WeightedGraphBuilder;
import com.example.subsystemdiscovery.leiden.ClusterAggregationUtil;
import com.example.subsystemdiscovery.leiden.LeidenAlgorithmUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SubsystemDiscoveryServiceTest {

    private GraphExtractionService graphExtractionService;
    private WeightedGraphBuilder weightedGraphBuilder;
    private LeidenAlgorithmUtil leidenAlgorithmUtil;
    private ClusterAggregationUtil clusterAggregationUtil;
    private SubsystemLabelService subsystemLabelService;
    private LlmSubsystemDiscoveryService llmSubsystemDiscoveryService;
    private TbNodeHistoryMapper tbNodeHistoryMapper;
    private SubsystemHistoryMapper subsystemHistoryMapper;
    private ObjectMapper objectMapper;

    private SubsystemDiscoveryService service;

    @BeforeEach
    public void setUp() {
        graphExtractionService = mock(GraphExtractionService.class);
        weightedGraphBuilder = mock(WeightedGraphBuilder.class);
        leidenAlgorithmUtil = mock(LeidenAlgorithmUtil.class);
        clusterAggregationUtil = mock(ClusterAggregationUtil.class);
        subsystemLabelService = mock(SubsystemLabelService.class);
        llmSubsystemDiscoveryService = mock(LlmSubsystemDiscoveryService.class);
        tbNodeHistoryMapper = mock(TbNodeHistoryMapper.class);
        subsystemHistoryMapper = mock(SubsystemHistoryMapper.class);
        objectMapper = new ObjectMapper();

        service = new SubsystemDiscoveryService(
                graphExtractionService,
                weightedGraphBuilder,
                leidenAlgorithmUtil,
                clusterAggregationUtil,
                subsystemLabelService,
                llmSubsystemDiscoveryService,
                tbNodeHistoryMapper,
                subsystemHistoryMapper,
                objectMapper
        );
    }

    @Test
    public void testGenerateSummaryScenario1_ExistingDiscovery_ExistingSummary() throws Exception {
        // Setup existing master run
        SubsystemRunMaster master = new SubsystemRunMaster();
        master.setDiscoveryRunId(100L);
        master.setAnalysisTime("2026-06-17 14:00:00");
        master.setRuns(10);
        master.setConsensusThreshold(0.7);
        master.setResolution(1.0);
        master.setTotalSubsystems(2);
        master.setAvgStabilityScore(0.85);

        SubsystemPersistenceDto persistenceDto = new SubsystemPersistenceDto(
                Collections.emptyList(),
                Collections.emptyList()
        );
        master.setDiscoveryResult(objectMapper.writeValueAsString(persistenceDto));

        // Mock database calls
        when(subsystemHistoryMapper.selectMasterById(100L)).thenReturn(master);
        when(tbNodeHistoryMapper.selectApplicationMetadata(master.getAnalysisTime()))
                .thenReturn(new ApplicationMetadata(1L, "TEST_APP"));
        when(subsystemHistoryMapper.selectLlmSummaryByConfig(100L, "gemini-flash", "MEDIUM_DETAILED"))
                .thenReturn("Existing Summary content");

        // Invoke method
        String response = service.generateSummary(100L, "gemini-flash", SummaryType.MEDIUM_DETAILED);

        // Verify results
        assertNotNull(response);
        assertEquals("Existing Summary content", response);

        // Verify LLM generate and DB insert were NOT called
        verify(llmSubsystemDiscoveryService, never()).summarise(any(), any(), any());
        verify(subsystemHistoryMapper, never()).insertLlmSummary(anyLong(), anyString(), anyString(), anyString());
    }

    @Test
    public void testGenerateSummaryScenario2_ExistingDiscovery_NewSummary() throws Exception {
        // Setup existing master run
        SubsystemRunMaster master = new SubsystemRunMaster();
        master.setDiscoveryRunId(200L);
        master.setAnalysisTime("2026-06-17 15:00:00");
        master.setRuns(20);
        master.setConsensusThreshold(0.8);
        master.setResolution(1.2);
        master.setTotalSubsystems(4);
        master.setAvgStabilityScore(0.92);

        SubsystemPersistenceDto persistenceDto = new SubsystemPersistenceDto(
                Collections.emptyList(),
                Collections.emptyList()
        );
        master.setDiscoveryResult(objectMapper.writeValueAsString(persistenceDto));

        // Mock database calls
        when(subsystemHistoryMapper.selectMasterById(200L)).thenReturn(master);
        when(tbNodeHistoryMapper.selectApplicationMetadata(master.getAnalysisTime()))
                .thenReturn(new ApplicationMetadata(1L, "TEST_APP"));
        when(subsystemHistoryMapper.selectLlmSummaryByConfig(200L, "gpt-4", "COMPLETE_DETAILED"))
                .thenReturn(null);
        when(llmSubsystemDiscoveryService.summarise(any(), any(), any()))
                .thenReturn("Newly generated LLM summary text");

        // Invoke method
        String response = service.generateSummary(200L, "gpt-4", SummaryType.COMPLETE_DETAILED);

        // Verify results
        assertNotNull(response);
        assertEquals("Newly generated LLM summary text", response);

        // Verify LLM summarise was invoked with correct arguments
        verify(llmSubsystemDiscoveryService, times(1))
                .summarise(any(), eq(SummaryType.COMPLETE_DETAILED), eq("gpt-4"));

        // Verify new summary was inserted into DB
        verify(subsystemHistoryMapper, times(1))
                .insertLlmSummary(200L, "gpt-4", "COMPLETE_DETAILED", "Newly generated LLM summary text");
    }
}
