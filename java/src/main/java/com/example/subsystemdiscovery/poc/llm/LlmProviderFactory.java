package com.example.subsystemdiscovery.poc.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LlmProviderFactory {

    private static final Logger log = LoggerFactory.getLogger(LlmProviderFactory.class);
    private final Map<String, LlmProvider> providers;

    public LlmProviderFactory(List<LlmProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(p -> p.getProviderName().toUpperCase(), Function.identity()));
        log.info("LlmProviderFactory initialized with providers: {}", providers.keySet());
    }

    public LlmProvider getProvider(String name) {
        log.info("Retrieving LLM provider: '{}'", name);
        LlmProvider provider = providers.get(name.toUpperCase());
        if (provider == null) {
            log.warn("LLM Provider '{}' not found. Falling back to MOCK provider.", name);
            return providers.get("MOCK");
        }
        return provider;
    }
}
