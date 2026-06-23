package com.example.subsystemdiscovery.llm;

import com.example.subsystemdiscovery.discovery.dto.SubsystemDiscoveryResponse;

public interface LlmProvider {
    String getProviderName();
    String generateSummary(SubsystemDiscoveryResponse response, String summaryType, String llmModel);
}
