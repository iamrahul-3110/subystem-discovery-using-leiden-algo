package com.example.subsystemdiscovery.poc.llm;

import com.example.subsystemdiscovery.subsystem.dto.SubsystemDiscoveryResponse;

public interface LlmProvider {
    String getProviderName();
    String generateSummary(SubsystemDiscoveryResponse response, String summaryType, String llmModel);
}
