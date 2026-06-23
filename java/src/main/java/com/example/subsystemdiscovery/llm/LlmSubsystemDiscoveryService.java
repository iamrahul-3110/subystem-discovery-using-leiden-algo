package com.example.subsystemdiscovery.llm;

import com.example.subsystemdiscovery.config.LlmProperties;
import com.example.subsystemdiscovery.llm.dto.LlmSummaryInput;
import com.example.subsystemdiscovery.discovery.dto.SummaryType;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LlmSubsystemDiscoveryService {

    private final LlmClientService llmClientService;
    private final LlmSummaryPromptBuilder promptBuilder;
    private final LlmProperties properties;

    public LlmSubsystemDiscoveryService(LlmClientService llmClientService,
                                        LlmSummaryPromptBuilder promptBuilder,
                                        LlmProperties properties) {
        this.llmClientService = llmClientService;
        this.promptBuilder = promptBuilder;
        this.properties = properties;
    }

    public String summarise(LlmSummaryInput input,
                            SummaryType summaryType,
                            String llmModel) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())
                || input == null) {
            return fallbackSummary();
        }

        SummaryType resolvedType = summaryType != null ? summaryType : SummaryType.MEDIUM_DETAILED;

        try {
            String prompt  = promptBuilder.build(input, resolvedType);
            JsonNode resp  = llmClientService.callLlm(prompt, llmModel);
            String content = llmClientService.extractContent(resp);

            if (StringUtils.hasText(content)) {
                return content.strip();
            }
            return fallbackSummary();

        } catch (Exception ex) {
            return fallbackSummary();
        }
    }

    private String fallbackSummary() {
        return "LLM architectural summary not available. The subsystem discovery result is available in the response.";
    }
}
