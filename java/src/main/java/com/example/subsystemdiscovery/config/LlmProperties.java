package com.example.subsystemdiscovery.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the custom LLM chat-completions API.
 *
 * <p>Maps to the {@code subsystem.llm.*} namespace in {@code application.yml}.
 *
 * <h3>Example application.yml</h3>
 * <pre>
 * subsystem:
 *   llm:
 *     enabled: true
 *     base-url: https://some.url/general/api/v1
 *     api-key: 5a612d87-efb4-4783-8e2e-764221ae6e01
 *     service-id: Rahul
 *     user-identifier: user.account@gmail.com
 *     model-id: "42"
 *     max-tokens: 24576
 *     temperature: 1.0
 *     top-k: 0
 *     connect-timeout-ms: 5000
 *     read-timeout-ms: 60000
 * </pre>
 */
@Data
@ConfigurationProperties(prefix = "subsystem.llm")
public class LlmProperties {

    private String openrouterApiKey;

    private String openrouterUrl;

    private String geminiApiKey;

    private String provider = "openai";

    private boolean enabled = true;

    private String baseUrl;

    private String apiKey;

    private String serviceId;

    private String userIdentifier;

    private String modelId;

    private String ollamaUrl =
            "http://localhost:11434";

    private int maxTokens = 24576;

    private double temperature = 0.7;

    private int topK = 0;

    private int connectTimeoutMs = 5000;

    private int readTimeoutMs = 60000;

}